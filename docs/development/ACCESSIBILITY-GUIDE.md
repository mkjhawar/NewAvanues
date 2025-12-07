# VoiceOS Accessibility Guide

**Version:** 1.0
**Author:** Manoj Jhawar
**Created:** 2025-11-09
**Phase:** 3 (Medium Priority)

---

## Overview

This guide provides comprehensive best practices for making VoiceOS accessible to users with disabilities, particularly users of screen readers and accessibility services like TalkBack.

### Why Accessibility Matters

VoiceOS is inherently an accessibility-focused application (voice control for device interaction). Ensuring the app itself is accessible creates a virtuous cycle where users who need accessibility features can configure and use the app effectively.

---

## Table of Contents

1. [Accessibility Basics](#accessibility-basics)
2. [Content Descriptions](#content-descriptions)
3. [Accessibility Actions](#accessibility-actions)
4. [Testing Accessibility](#testing-accessibility)
5. [Common Patterns](#common-patterns)
6. [Best Practices](#best-practices)
7. [VoiceOS-Specific Guidelines](#voiceos-specific-guidelines)

---

## Accessibility Basics

### Key Principles

1. **Perceivable**: Information must be presentable to users in ways they can perceive
2. **Operable**: UI components must be operable (keyboard, voice, touch)
3. **Understandable**: Information and UI operation must be understandable
4. **Robust**: Content must be robust enough to work with assistive technologies

### Android Accessibility Framework

Android provides several ways to make apps accessible:
- **Content descriptions** (`contentDescription` attribute)
- **Labels** (`labelFor` attribute)
- **Accessibility actions** (custom actions for screen readers)
- **Live regions** (announce dynamic content changes)
- **Accessibility services** (custom accessibility features)

---

## Content Descriptions

### What Are Content Descriptions?

Content descriptions provide text alternatives for visual UI elements. Screen readers like TalkBack read these descriptions aloud to help users understand the purpose of UI elements.

### When to Add Content Descriptions

**Always add for:**
- ImageButton, ImageView (icons, logos)
- Buttons with only icons
- Custom views
- Interactive elements without visible text
- Decorative elements that convey meaning

**Don't add for:**
- Purely decorative images
- Elements with visible text labels
- Text views (already read by screen readers)

### XML Attributes

#### android:contentDescription

```xml
<!-- ✅ CORRECT: Clear, concise description -->
<ImageButton
    android:id="@+id/btn_enable_voice"
    android:src="@drawable/ic_microphone"
    android:contentDescription="@string/accessibility_button_enable"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content" />

<!-- ❌ INCORRECT: Hardcoded description -->
<ImageButton
    android:src="@drawable/ic_microphone"
    android:contentDescription="Enable voice control"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content" />
```

**strings.xml:**
```xml
<string name="accessibility_button_enable">Enable voice control</string>
<string name="accessibility_button_disable">Disable voice control</string>
```

#### android:importantForAccessibility

Controls whether an element is exposed to accessibility services:

```xml
<!-- Make element accessible (default for most views) -->
<View android:importantForAccessibility="yes" />

<!-- Hide from accessibility (decorative only) -->
<ImageView
    android:src="@drawable/decorative_background"
    android:importantForAccessibility="no" />

<!-- Auto (system decides) -->
<View android:importantForAccessibility="auto" />

<!-- Hide descendants (group elements) -->
<LinearLayout android:importantForAccessibility="noHideDescendants">
    <!-- Children won't be accessible -->
</LinearLayout>
```

### Programmatic Content Descriptions

#### Kotlin

```kotlin
// Set content description
button.contentDescription = getString(R.string.accessibility_button_enable)

// Dynamic content description
val count = commandsExecuted
textView.contentDescription = resources.getQuantityString(
    R.plurals.commands_executed,
    count,
    count
)

// Remove content description (for decorative elements)
imageView.contentDescription = null
// OR
imageView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
```

#### ViewCompat for Compatibility

```kotlin
import androidx.core.view.ViewCompat

ViewCompat.setAccessibilityDelegate(view, object : AccessibilityDelegateCompat() {
    override fun onInitializeAccessibilityNodeInfo(
        host: View,
        info: AccessibilityNodeInfoCompat
    ) {
        super.onInitializeAccessibilityNodeInfo(host, info)
        info.contentDescription = "Custom accessibility description"
    }
})
```

---

## Accessibility Actions

### Custom Actions

Provide alternative ways to interact with UI elements:

```kotlin
ViewCompat.setAccessibilityDelegate(button, object : AccessibilityDelegateCompat() {
    override fun onInitializeAccessibilityNodeInfo(
        host: View,
        info: AccessibilityNodeInfoCompat
    ) {
        super.onInitializeAccessibilityNodeInfo(host, info)

        // Add custom action
        info.addAction(
            AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                R.id.action_enable,
                getString(R.string.accessibility_action_enable)
            )
        )
    }

    override fun performAccessibilityAction(
        host: View,
        action: Int,
        args: Bundle?
    ): Boolean {
        when (action) {
            R.id.action_enable -> {
                enableVoiceControl()
                return true
            }
        }
        return super.performAccessibilityAction(host, action, args)
    }
})
```

### Standard Actions

Use standard Android actions when possible:

```kotlin
// Clickable
view.isClickable = true

// Long clickable
view.isLongClickable = true

// Focusable
view.isFocusable = true
```

---

## Testing Accessibility

### 1. Enable TalkBack

**Settings → Accessibility → TalkBack → Turn on**

Navigate your app with TalkBack enabled:
- Swipe right/left to move between elements
- Double-tap to activate
- Listen to what TalkBack announces

### 2. Accessibility Scanner

Install Google's Accessibility Scanner from Play Store:
- Scans your app for accessibility issues
- Provides suggestions for improvements
- Checks contrast ratios, touch target sizes, content descriptions

### 3. Espresso Accessibility Checks

```kotlin
@Test
fun checkAccessibility() {
    onView(withId(R.id.main_layout))
        .check(AccessibilityChecks())
}
```

### 4. Manual Testing Checklist

- [ ] All interactive elements have content descriptions
- [ ] Touch targets are at least 48dp × 48dp
- [ ] Text contrast ratio is at least 4.5:1
- [ ] App is navigable using TalkBack
- [ ] Dynamic content changes are announced
- [ ] Error messages are accessible
- [ ] Forms have proper labels

---

## Common Patterns

### Button with Icon Only

```xml
<ImageButton
    android:id="@+id/btn_microphone"
    android:src="@drawable/ic_microphone"
    android:contentDescription="@string/accessibility_enable_microphone"
    android:minWidth="48dp"
    android:minHeight="48dp" />
```

### Toggle Button

```kotlin
class VoiceToggleButton : AppCompatButton {
    private var isVoiceEnabled = false

    fun setVoiceEnabled(enabled: Boolean) {
        isVoiceEnabled = enabled
        updateContentDescription()
        // Update UI
    }

    private fun updateContentDescription() {
        contentDescription = if (isVoiceEnabled) {
            context.getString(R.string.accessibility_button_disable)
        } else {
            context.getString(R.string.accessibility_button_enable)
        }
    }
}
```

### Dynamic Content

```kotlin
// Announce changes to screen reader
textView.announceForAccessibility(
    getString(R.string.command_executed, commandName)
)

// Live region (automatically announces changes)
textView.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
```

### Custom View

```kotlin
class CustomCursorView : View {
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)

        info.contentDescription = context.getString(
            R.string.accessibility_cursor_position,
            cursorX,
            cursorY
        )

        // Add custom actions
        info.addAction(
            AccessibilityNodeInfo.AccessibilityAction(
                R.id.action_move_cursor,
                context.getString(R.string.accessibility_action_move_cursor)
            )
        )
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        when (action) {
            R.id.action_move_cursor -> {
                // Handle cursor movement
                return true
            }
        }
        return super.performAccessibilityAction(action, arguments)
    }
}
```

### Grouping Elements

```xml
<!-- Group related elements for better navigation -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:contentDescription="@string/accessibility_command_group"
    android:focusable="true">

    <TextView
        android:text="Command:"
        android:importantForAccessibility="no" />

    <TextView
        android:id="@+id/command_name"
        android:importantForAccessibility="no" />
</LinearLayout>
```

---

## Best Practices

### 1. Write Clear Descriptions

```kotlin
// ❌ BAD: Too technical
button.contentDescription = "btn_enable_vc"

// ❌ BAD: Too verbose
button.contentDescription = "This button enables the voice control feature which allows you to control your device using voice commands"

// ✅ GOOD: Clear and concise
button.contentDescription = getString(R.string.accessibility_button_enable)
```

### 2. Use Action Verbs

```xml
<!-- ✅ GOOD: Uses action verb -->
<string name="accessibility_button_enable">Enable voice control</string>

<!-- ❌ BAD: Describes state, not action -->
<string name="accessibility_button_enable">Voice control enabled</string>
```

### 3. Provide Context

```kotlin
// ❌ BAD: No context
imageView.contentDescription = "Checkmark"

// ✅ GOOD: Provides context
imageView.contentDescription = "Voice command successfully executed"
```

### 4. Update Dynamically

```kotlin
// Update description when state changes
fun updateMicrophoneButton(isListening: Boolean) {
    micButton.contentDescription = if (isListening) {
        getString(R.string.accessibility_stop_listening)
    } else {
        getString(R.string.accessibility_start_listening)
    }
}
```

### 5. Avoid Redundancy

```xml
<!-- ❌ BAD: Redundant (button already announces it's a button) -->
<Button
    android:text="Submit"
    android:contentDescription="Submit button" />

<!-- ✅ GOOD: Text is sufficient -->
<Button
    android:text="Submit" />

<!-- ✅ GOOD: Icon button needs description -->
<ImageButton
    android:src="@drawable/ic_submit"
    android:contentDescription="@string/accessibility_submit" />
```

### 6. Touch Target Size

```xml
<!-- Ensure minimum 48dp × 48dp touch targets -->
<ImageButton
    android:src="@drawable/ic_small_icon"
    android:contentDescription="@string/accessibility_action"
    android:minWidth="48dp"
    android:minHeight="48dp"
    android:scaleType="center" />
```

### 7. Color Contrast

```xml
<!-- Ensure sufficient contrast (4.5:1 for normal text, 3:1 for large text) -->
<TextView
    android:text="Important message"
    android:textColor="#000000"
    android:background="#FFFFFF" /> <!-- 21:1 contrast ratio -->
```

---

## VoiceOS-Specific Guidelines

### Voice Command Feedback

```kotlin
// Announce command execution to screen reader
fun executeCommand(command: String) {
    // Execute command
    val result = commandExecutor.execute(command)

    // Announce result
    val announcement = if (result.success) {
        getString(R.string.accessibility_command_success, command)
    } else {
        getString(R.string.accessibility_command_failed, command, result.error)
    }

    rootView.announceForAccessibility(announcement)
}
```

**strings.xml:**
```xml
<string name="accessibility_command_success">Command %s executed successfully</string>
<string name="accessibility_command_failed">Command %s failed: %s</string>
```

### Cursor Position

```kotlin
class AccessibilityCursorManager(private val context: Context) {
    fun announceCursorPosition(x: Int, y: Int) {
        val announcement = context.getString(
            R.string.accessibility_cursor_move,
            x,
            y
        )
        rootView.announceForAccessibility(announcement)
    }
}
```

**strings.xml:**
```xml
<string name="accessibility_cursor_move">Cursor moved to %1$d, %2$d</string>
```

### Service Status

```kotlin
class VoiceOSService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        announceServiceStatus(true)
    }

    override fun onInterrupt() {
        announceServiceStatus(false)
    }

    private fun announceServiceStatus(connected: Boolean) {
        val message = if (connected) {
            getString(R.string.accessibility_service_connected)
        } else {
            getString(R.string.accessibility_service_disconnected)
        }

        // Announce to user
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
```

### Settings Screen

```xml
<!-- Accessibility-friendly settings -->
<PreferenceScreen>
    <SwitchPreferenceCompat
        android:key="enable_voice_control"
        android:title="@string/settings_enable_voice"
        android:summary="@string/settings_enable_voice_summary"
        android:defaultValue="true" />

    <SeekBarPreference
        android:key="command_cache_duration"
        android:title="@string/settings_cache_duration"
        android:summary="@string/settings_cache_duration_summary"
        android:min="1"
        android:max="60"
        android:defaultValue="30" />
</PreferenceScreen>
```

---

## Resources

### Android Documentation

- [Accessibility Developer Guide](https://developer.android.com/guide/topics/ui/accessibility)
- [Accessibility Principles](https://developer.android.com/guide/topics/ui/accessibility/principles)
- [Testing Accessibility](https://developer.android.com/guide/topics/ui/accessibility/testing)
- [Accessibility Scanner](https://support.google.com/accessibility/android/answer/6376570)

### WCAG Guidelines

- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [Understanding WCAG](https://www.w3.org/WAI/WCAG21/Understanding/)

### Tools

- **TalkBack**: Built-in Android screen reader
- **Accessibility Scanner**: Google's accessibility testing app
- **Espresso**: Automated accessibility testing
- **Color Contrast Analyzer**: Check color contrast ratios

---

## Checklist

### Pre-Release Accessibility Checklist

- [ ] All ImageButton and ImageView elements have contentDescription
- [ ] Decorative images have importantForAccessibility="no"
- [ ] All interactive elements are at least 48dp × 48dp
- [ ] Text contrast ratio meets WCAG AA standard (4.5:1)
- [ ] App is fully navigable with TalkBack enabled
- [ ] Dynamic content changes are announced
- [ ] Error messages have proper accessibility announcements
- [ ] Form fields have labels (android:labelFor)
- [ ] Custom views implement accessibility properly
- [ ] Tested with Accessibility Scanner (0 critical issues)
- [ ] All user-facing strings are in string resources
- [ ] Accessibility live regions used where appropriate

---

## Example Implementation

### Complete Accessible Button

**Layout (XML):**
```xml
<ImageButton
    android:id="@+id/btn_toggle_voice"
    android:src="@drawable/ic_microphone"
    android:contentDescription="@string/accessibility_button_enable"
    android:minWidth="48dp"
    android:minHeight="48dp"
    android:background="?attr/selectableItemBackgroundBorderless"
    android:scaleType="center"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content" />
```

**Kotlin:**
```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var voiceButton: ImageButton
    private var isVoiceEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        voiceButton = findViewById(R.id.btn_toggle_voice)
        voiceButton.setOnClickListener {
            toggleVoiceControl()
        }

        updateVoiceButton()
    }

    private fun toggleVoiceControl() {
        isVoiceEnabled = !isVoiceEnabled
        updateVoiceButton()

        // Announce change to screen reader
        val announcement = if (isVoiceEnabled) {
            getString(R.string.accessibility_voice_enabled)
        } else {
            getString(R.string.accessibility_voice_disabled)
        }
        voiceButton.announceForAccessibility(announcement)
    }

    private fun updateVoiceButton() {
        // Update visual state
        voiceButton.setImageResource(
            if (isVoiceEnabled) {
                R.drawable.ic_microphone_on
            } else {
                R.drawable.ic_microphone_off
            }
        )

        // Update content description
        voiceButton.contentDescription = if (isVoiceEnabled) {
            getString(R.string.accessibility_button_disable)
        } else {
            getString(R.string.accessibility_button_enable)
        }
    }
}
```

**strings.xml:**
```xml
<string name="accessibility_button_enable">Enable voice control</string>
<string name="accessibility_button_disable">Disable voice control</string>
<string name="accessibility_voice_enabled">Voice control enabled</string>
<string name="accessibility_voice_disabled">Voice control disabled</string>
```

---

**End of Guide**

**Last Updated:** 2025-11-09
**Version:** 1.0
**Phase:** 3 (Medium Priority)
