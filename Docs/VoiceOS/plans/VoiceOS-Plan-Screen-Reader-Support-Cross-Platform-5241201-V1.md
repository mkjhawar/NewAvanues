# Implementation Plan: Screen Reader Support (Cross-Platform)
**Scope:** VoiceOS + MagicUI
**Platforms:** Android, iOS, Desktop (Windows/macOS/Linux), Web
**Date:** 2025-12-24
**Version:** 1.0

---

## Table of Contents

1. [Overview](#overview)
2. [Platform APIs](#platform-apis)
3. [MagicUI Framework Integration](#magicui-framework-integration)
4. [VoiceOS Application Integration](#voiceos-application-integration)
5. [Implementation Phases](#implementation-phases)
6. [Code Examples](#code-examples)
7. [Testing Strategy](#testing-strategy)
8. [WCAG Compliance](#wcag-compliance)

---

## Overview

### **Goals**

1. **MagicUI Framework:** Built-in screen reader support for all components
2. **VoiceOS App:** Fully accessible to screen reader users on all platforms
3. **WCAG 2.1 Level AA:** Meet international accessibility standards
4. **Universal Design:** Works with voice control AND screen readers simultaneously

### **Success Metrics**

| Metric | Target | Measurement |
|--------|--------|-------------|
| WCAG 2.1 Level AA Compliance | 100% | Automated + manual audit |
| Screen reader compatibility | All major readers | Manual testing |
| Keyboard navigation | 100% operable | Automated tests |
| Focus indicators | Visible on all elements | Visual inspection |
| Semantic HTML/ARIA | 100% coverage | Automated linting |

### **Timeline**

- **Phase 1 (Android):** 2 weeks
- **Phase 2 (iOS):** 2 weeks
- **Phase 3 (Desktop):** 3 weeks
- **Phase 4 (Web):** 2 weeks
- **Testing & Compliance:** 2 weeks
- **Total:** 11 weeks (2.75 months)

---

## Platform APIs

### **Android: Accessibility API**

**Framework:** Android Accessibility Framework
**Primary Screen Reader:** TalkBack
**Key Concepts:** AccessibilityNodeInfo, ContentDescription, AccessibilityEvent

#### **Jetpack Compose Semantics**

```kotlin
import androidx.compose.ui.semantics.*

@Composable
fun AccessibleButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.semantics {
            // 1. Content description (what is this?)
            contentDescription = "$text button"

            // 2. Role (what type of element?)
            role = Role.Button

            // 3. State (current state)
            stateDescription = "Enabled"

            // 4. Actions (what can you do?)
            onClick(label = "Activate") {
                onClick()
                true
            }

            // 5. Live region (announce changes)
            liveRegion = LiveRegionMode.Polite
        }
    ) {
        Text(text)
    }
}
```

**TalkBack Behavior:**
```
User focuses button → "Submit button, button, double-tap to activate"
User double-taps → Button clicked → "Activated"
```

#### **Available Semantic Properties**

```kotlin
Modifier.semantics {
    // DESCRIPTIONS
    contentDescription = "Description text"

    // ROLES
    role = Role.Button         // Button, Checkbox, RadioButton, Image, Tab, etc.

    // STATES
    stateDescription = "Selected"
    disabled()                 // Mark as disabled
    selected = true            // Mark as selected

    // ACTIONS
    onClick { }                // Click action
    onLongClick { }            // Long-press action

    // STRUCTURE
    heading()                  // Mark as heading
    collectionInfo = CollectionInfo(rowCount = 5, columnCount = 2)
    collectionItemInfo = CollectionItemInfo(rowIndex = 0, columnIndex = 0)

    // LIVE REGIONS
    liveRegion = LiveRegionMode.Polite  // Announce changes

    // VISIBILITY
    invisibleToUser()          // Hide from screen readers

    // CUSTOM
    customActions = listOf(
        CustomAccessibilityAction("Custom action") { /* ... */ }
    )
}
```

---

### **iOS: UIAccessibility**

**Framework:** UIKit/SwiftUI Accessibility
**Primary Screen Reader:** VoiceOver
**Key Concepts:** accessibilityLabel, accessibilityHint, accessibilityTraits

#### **SwiftUI Accessibility Modifiers**

```swift
import SwiftUI

struct AccessibleButton: View {
    let text: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
        }
        // 1. Label (what is this?)
        .accessibilityLabel("\(text) button")

        // 2. Hint (how to use it?)
        .accessibilityHint("Double-tap to activate")

        // 3. Traits (what type/state?)
        .accessibilityAddTraits(.isButton)

        // 4. Value (current value)
        .accessibilityValue("Enabled")

        // 5. Actions (custom actions)
        .accessibilityAction(named: "Custom Action") {
            // Custom action
        }
    }
}
```

**VoiceOver Behavior:**
```
User focuses button → "Submit button, button, double-tap to activate"
User double-taps → Button clicked → VoiceOver plays success sound
```

#### **Available Accessibility Modifiers**

```swift
.accessibilityLabel("Description")           // What is this?
.accessibilityHint("How to use")             // How to interact?
.accessibilityValue("Current value")         // Current state/value

// Traits (combine multiple)
.accessibilityAddTraits(.isButton)           // Button
.accessibilityAddTraits(.isSelected)         // Selected state
.accessibilityAddTraits(.isHeader)           // Heading
.accessibilityAddTraits(.updatesFrequently)  // Live region

// Structure
.accessibilityElement(children: .combine)    // Combine children into one
.accessibilityElement(children: .ignore)     // Ignore children
.accessibilityElement(children: .contain)    // Preserve children

// Actions
.accessibilityAction(named: "Action") { }    // Custom action
.accessibilityScrollAction { }               // Scroll action

// Sorting
.accessibilitySortPriority(1)                // Focus order

// Hide from VoiceOver
.accessibilityHidden(true)
```

---

### **Desktop: Platform-Specific APIs**

#### **macOS: NSAccessibility**

**Primary Screen Reader:** VoiceOver (macOS)
**Framework:** AppKit NSAccessibility

```swift
import AppKit

class AccessibleButton: NSButton {
    override var accessibilityRole: NSAccessibility.Role? {
        get { .button }
        set { }
    }

    override var accessibilityLabel: String? {
        get { "\(title) button" }
        set { }
    }

    override var accessibilityValue: Any? {
        get { isEnabled ? "Enabled" : "Disabled" }
        set { }
    }

    override func accessibilityPerformPress() -> Bool {
        performClick(nil)
        return true
    }
}
```

#### **Windows: UI Automation**

**Primary Screen Readers:** NVDA, JAWS, Narrator
**Framework:** UI Automation API

```kotlin
// Via Compose Desktop (JVM)
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.AccessibilityProvider

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AccessibleButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.semantics {
            // Compose Desktop maps to UI Automation automatically
            contentDescription = "$text button"
            role = Role.Button
        }
    ) {
        Text(text)
    }
}
```

**Note:** Compose Desktop automatically maps Compose semantics to Windows UI Automation.

#### **Linux: AT-SPI**

**Primary Screen Reader:** Orca
**Framework:** ATK/AT-SPI

```kotlin
// Via Compose Desktop (JVM)
// Same as Windows - Compose Desktop handles platform mapping
@Composable
fun AccessibleButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.semantics {
            contentDescription = "$text button"
            role = Role.Button
        }
    ) {
        Text(text)
    }
}
```

**Note:** Compose Desktop maps to GTK accessibility (ATK/AT-SPI) on Linux.

---

### **Web: ARIA Attributes**

**Primary Screen Readers:** NVDA, JAWS, VoiceOver (via browser)
**Framework:** ARIA (Accessible Rich Internet Applications)

```tsx
// React + TypeScript
interface AccessibleButtonProps {
  text: string;
  onClick: () => void;
}

function AccessibleButton({ text, onClick }: AccessibleButtonProps) {
  return (
    <button
      onClick={onClick}
      aria-label={`${text} button`}
      aria-describedby="button-hint"
      role="button"
      tabIndex={0}
    >
      {text}
      <span id="button-hint" className="sr-only">
        Press Enter or Space to activate
      </span>
    </button>
  );
}
```

**Screen Reader Behavior:**
```
User focuses button → "Submit button, button, Press Enter or Space to activate"
User presses Enter → Button clicked → Focus moves/announcement plays
```

#### **ARIA Attributes Reference**

```html
<!-- Roles -->
<div role="button">          <!-- Button -->
<div role="checkbox">        <!-- Checkbox -->
<div role="radiogroup">      <!-- Radio group -->
<div role="dialog">          <!-- Modal dialog -->
<div role="alert">           <!-- Alert/notification -->
<div role="navigation">      <!-- Navigation landmark -->
<div role="main">            <!-- Main content landmark -->

<!-- Labels & Descriptions -->
<button aria-label="Close">              <!-- Accessible name -->
<button aria-labelledby="label-id">     <!-- Reference to label -->
<button aria-describedby="desc-id">     <!-- Additional description -->

<!-- States -->
<button aria-pressed="true">             <!-- Toggle button state -->
<input aria-checked="true">              <!-- Checkbox state -->
<input aria-disabled="true">             <!-- Disabled state -->
<div aria-expanded="true">               <!-- Expanded/collapsed -->
<div aria-hidden="true">                 <!-- Hidden from screen readers -->

<!-- Live Regions -->
<div aria-live="polite">                 <!-- Announce changes (politely) -->
<div aria-live="assertive">              <!-- Announce changes (immediately) -->
<div aria-atomic="true">                 <!-- Announce entire region -->

<!-- Relationships -->
<input aria-controls="panel-id">         <!-- Controls element -->
<li aria-posinset="2" aria-setsize="5">  <!-- Position in set -->

<!-- Properties -->
<input aria-required="true">             <!-- Required field -->
<input aria-invalid="true">              <!-- Validation error -->
<input aria-readonly="true">             <!-- Read-only -->
```

---

## MagicUI Framework Integration

### **Architecture: Unified Semantics API**

**Goal:** Single API that maps to all platform screen reader systems

```kotlin
// Common interface (shared KMP code)
interface AccessibilitySemantics {
    val label: String?
    val hint: String?
    val role: AccessibilityRole
    val state: AccessibilityState
    val actions: List<AccessibilityAction>
}

enum class AccessibilityRole {
    BUTTON,
    CHECKBOX,
    RADIO_BUTTON,
    TEXT_FIELD,
    IMAGE,
    HEADING,
    LINK,
    LIST,
    LIST_ITEM,
    // ... more roles
}

data class AccessibilityState(
    val isEnabled: Boolean = true,
    val isSelected: Boolean = false,
    val isChecked: Boolean? = null,  // null for non-checkable
    val value: String? = null,
    val valueRange: ClosedFloatingPointRange<Float>? = null
)

data class AccessibilityAction(
    val name: String,
    val action: () -> Boolean
)
```

### **Platform Mapping**

```kotlin
// Android implementation (androidMain)
actual fun Modifier.accessibilitySemantics(
    semantics: AccessibilitySemantics
): Modifier = this.semantics {
    semantics.label?.let { contentDescription = it }
    role = when (semantics.role) {
        AccessibilityRole.BUTTON -> Role.Button
        AccessibilityRole.CHECKBOX -> Role.Checkbox
        // ... map all roles
    }
    stateDescription = semantics.state.toString()
    // ... map actions
}

// iOS implementation (iosMain)
actual fun Modifier.accessibilitySemantics(
    semantics: AccessibilitySemantics
): Modifier {
    // Map to SwiftUI accessibility modifiers
    // Implementation via expect/actual + Swift interop
}

// Desktop implementation (jvmMain)
actual fun Modifier.accessibilitySemantics(
    semantics: AccessibilitySemantics
): Modifier = this.semantics {
    // Compose Desktop handles mapping to platform APIs
    contentDescription = semantics.label
    role = mapRole(semantics.role)
    // ... rest of mapping
}

// Web implementation (jsMain)
actual fun Modifier.accessibilitySemantics(
    semantics: AccessibilitySemantics
): Modifier {
    // Map to ARIA attributes via Compose for Web
}
```

### **MagicUI Component Example**

```kotlin
// commonMain
@Composable
fun MagicButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.accessibilitySemantics(
            AccessibilitySemantics(
                label = "$text button",
                hint = "Double-tap to activate",
                role = AccessibilityRole.BUTTON,
                state = AccessibilityState(isEnabled = enabled),
                actions = listOf(
                    AccessibilityAction("Activate") {
                        onClick()
                        true
                    }
                )
            )
        )
    ) {
        Text(text)
    }
}
```

**Result:** Works on all platforms with appropriate screen reader behavior!

---

## VoiceOS Application Integration

### **Phase 1: Overlay Accessibility (Android)**

**Current Issue:** Overlays are visually-only, no TalkBack support

#### **Task 1.1: NumberedSelectionOverlay**

```kotlin
@Composable
fun NumberedSelectionOverlay(elements: List<UIElement>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                // Announce overlay appearance
                liveRegion = LiveRegionMode.Polite
                contentDescription = buildString {
                    append("Voice selection overlay. ")
                    append("${elements.size} numbered elements available. ")
                    append("Say 'select' followed by a number, or swipe to explore.")
                }
                role = Role.Dialog

                // Custom action to list all elements
                customActions = listOf(
                    CustomAccessibilityAction("List all elements") {
                        // Announce all elements
                        announceForAccessibility(
                            elements.mapIndexed { idx, elem ->
                                "Element ${idx + 1}: ${elem.text ?: elem.className}"
                            }.joinToString(". ")
                        )
                        true
                    }
                )
            }
    ) {
        elements.forEachIndexed { index, element ->
            NumberBadge(
                number = index + 1,
                element = element,
                modifier = Modifier
                    .offset(
                        x = element.bounds.left.dp,
                        y = element.bounds.top.dp
                    )
                    .semantics {
                        contentDescription = buildString {
                            append("Element ${index + 1}")
                            element.text?.let { append(": $it") }
                            element.contentDescription?.let { append(", $it") }
                            append(". Say 'select ${index + 1}' or double-tap.")
                        }
                        role = Role.Button
                        onClick {
                            selectElement(index + 1)
                            true
                        }
                    }
            )
        }
    }
}
```

**TalkBack Experience:**
1. Overlay appears → "Voice selection overlay. 5 numbered elements available. Say 'select' followed by a number, or swipe to explore."
2. User swipes → "Element 1: Submit button. Say 'select 1' or double-tap."
3. User double-taps OR says "select 1" → Element selected
4. Overlay dismisses → "Selection confirmed"

#### **Task 1.2: ConfidenceOverlay**

```kotlin
@Composable
fun ConfidenceOverlay(confidence: Float) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(getConfidenceColor(confidence), shape = CircleShape)
            .semantics {
                contentDescription = "Voice recognition confidence: ${(confidence * 100).toInt()} percent"
                role = Role.Image
                liveRegion = LiveRegionMode.Polite

                // Provide context about confidence level
                stateDescription = when {
                    confidence >= 0.9 -> "Excellent confidence"
                    confidence >= 0.7 -> "Good confidence"
                    confidence >= 0.5 -> "Fair confidence"
                    else -> "Low confidence"
                }
            }
    ) {
        Text(
            "${(confidence * 100).toInt()}%",
            modifier = Modifier.semantics {
                // Hide text from TalkBack (already in contentDescription)
                invisibleToUser()
            }
        )
    }
}
```

**TalkBack Experience:**
- Overlay appears → "Voice recognition confidence: 87 percent. Good confidence."
- Confidence changes → "Voice recognition confidence: 92 percent. Excellent confidence."

#### **Task 1.3: ContextMenuOverlay**

```kotlin
@Composable
fun ContextMenuOverlay(items: List<MenuItem>) {
    Dialog(
        onDismissRequest = { /* dismiss */ }
    ) {
        Card(
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "Context menu with ${items.size} options"
                role = Role.Dialog
            }
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    MenuItem(
                        item = item,
                        modifier = Modifier.semantics {
                            contentDescription = "${item.label}. Option ${index + 1} of ${items.size}."
                            role = Role.Button

                            // Mark as selected if applicable
                            if (item.isSelected) {
                                selected = true
                                stateDescription = "Selected"
                            }

                            onClick {
                                item.action()
                                true
                            }
                        }
                    )
                }
            }
        }
    }
}
```

**TalkBack Experience:**
1. Menu appears → "Context menu with 3 options"
2. User swipes → "Copy. Option 1 of 3. Button."
3. User swipes → "Paste. Option 2 of 3. Button."
4. User double-taps → Action executed → "Paste completed"

---

### **Phase 2: iOS VoiceOver Support**

**Location:** `Modules/VoiceOS/apps/VoiceOSCore/src/iosMain/`

#### **iOS Accessibility Implementation**

```swift
// VoiceOSOverlay.swift
import SwiftUI

struct NumberedSelectionOverlay: View {
    let elements: [UIElement]

    var body: some View {
        ZStack {
            ForEach(elements.indices, id: \.self) { index in
                NumberBadge(
                    number: index + 1,
                    element: elements[index]
                )
                .position(
                    x: elements[index].bounds.midX,
                    y: elements[index].bounds.midY
                )
                // VoiceOver accessibility
                .accessibilityLabel(makeAccessibilityLabel(for: index))
                .accessibilityHint("Double-tap to select, or say 'select \(index + 1)'")
                .accessibilityAddTraits(.isButton)
                .accessibilityAction {
                    selectElement(index + 1)
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Voice selection overlay")
        .accessibilityValue("\(elements.count) numbered elements available")
        .accessibilityAddTraits(.updatesFrequently)
    }

    private func makeAccessibilityLabel(for index: Int) -> String {
        let element = elements[index]
        var label = "Element \(index + 1)"
        if let text = element.text {
            label += ": \(text)"
        }
        return label
    }
}
```

**VoiceOver Experience:**
- Identical to TalkBack (consistent cross-platform UX)

---

### **Phase 3: Desktop Screen Reader Support**

**Platforms:** Windows (NVDA/JAWS/Narrator), macOS (VoiceOver), Linux (Orca)

#### **Compose Desktop Accessibility**

```kotlin
// Desktop implementation (jvmMain)
@Composable
fun NumberedSelectionOverlay(elements: List<UIElement>) {
    Dialog(
        onDismissRequest = { /* dismiss */ }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    // Compose Desktop maps to platform APIs automatically
                    contentDescription = "Voice selection overlay with ${elements.size} elements"
                    role = Role.Dialog
                    liveRegion = LiveRegionMode.Polite
                }
        ) {
            elements.forEachIndexed { index, element ->
                NumberBadge(
                    number = index + 1,
                    element = element,
                    modifier = Modifier.semantics {
                        contentDescription = "Element ${index + 1}: ${element.text ?: "unlabeled"}"
                        role = Role.Button
                        onClick {
                            selectElement(index + 1)
                            true
                        }
                    }
                )
            }
        }
    }
}
```

**Platform Mapping:**
- **Windows:** Maps to UI Automation → Works with NVDA, JAWS, Narrator
- **macOS:** Maps to NSAccessibility → Works with VoiceOver
- **Linux:** Maps to ATK/AT-SPI → Works with Orca

---

### **Phase 4: Web Accessibility (WebAvanue)**

**Framework:** React + TypeScript + ARIA

#### **Web Component Implementation**

```tsx
// NumberedSelectionOverlay.tsx
import React, { useEffect, useRef } from 'react';

interface Props {
  elements: UIElement[];
  onSelect: (index: number) => void;
}

export function NumberedSelectionOverlay({ elements, onSelect }: Props) {
  const overlayRef = useRef<HTMLDivElement>(null);

  // Announce overlay appearance
  useEffect(() => {
    if (overlayRef.current) {
      const announcement = `Voice selection overlay with ${elements.length} elements`;
      announceToScreenReader(announcement);
    }
  }, [elements.length]);

  return (
    <div
      ref={overlayRef}
      role="dialog"
      aria-label="Voice selection overlay"
      aria-describedby="overlay-instructions"
      aria-live="polite"
      className="numbered-selection-overlay"
    >
      <div id="overlay-instructions" className="sr-only">
        {elements.length} numbered elements available.
        Say 'select' followed by a number, or use Tab to navigate.
      </div>

      {elements.map((element, index) => (
        <NumberBadge
          key={index}
          number={index + 1}
          element={element}
          onSelect={() => onSelect(index + 1)}
        />
      ))}
    </div>
  );
}

interface BadgeProps {
  number: number;
  element: UIElement;
  onSelect: () => void;
}

function NumberBadge({ number, element, onSelect }: BadgeProps) {
  return (
    <button
      onClick={onSelect}
      aria-label={`Element ${number}: ${element.text || 'unlabeled'}`}
      aria-describedby={`badge-hint-${number}`}
      role="button"
      tabIndex={0}
      className="number-badge"
      style={{
        left: element.bounds.left,
        top: element.bounds.top
      }}
    >
      {number}
      <span id={`badge-hint-${number}`} className="sr-only">
        Say 'select {number}' or press Enter
      </span>
    </button>
  );
}

// Utility to announce to screen readers
function announceToScreenReader(message: string) {
  const announcement = document.createElement('div');
  announcement.setAttribute('role', 'status');
  announcement.setAttribute('aria-live', 'polite');
  announcement.setAttribute('aria-atomic', 'true');
  announcement.className = 'sr-only';
  announcement.textContent = message;

  document.body.appendChild(announcement);
  setTimeout(() => document.body.removeChild(announcement), 1000);
}
```

**CSS for Screen Reader-Only Content:**

```css
/* Hide visually but keep for screen readers */
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border-width: 0;
}

/* Show on focus (for keyboard navigation) */
.sr-only-focusable:focus {
  position: static;
  width: auto;
  height: auto;
  overflow: visible;
  clip: auto;
  white-space: normal;
}
```

---

## Implementation Phases

### **Phase 1: Android TalkBack (2 weeks)**

**Week 1:**
- [ ] Task 1.1: Add semantics to NumberedSelectionOverlay
- [ ] Task 1.2: Add semantics to ConfidenceOverlay
- [ ] Task 1.3: Add semantics to ContextMenuOverlay
- [ ] Task 1.4: Add semantics to CommandStatusOverlay
- [ ] Task 1.5: Test with TalkBack enabled

**Week 2:**
- [ ] Task 1.6: Add focus management
- [ ] Task 1.7: Add keyboard navigation
- [ ] Task 1.8: Add custom accessibility actions
- [ ] Task 1.9: Accessibility audit (automated)
- [ ] Task 1.10: User testing with blind users

**Deliverables:**
- ✅ All VoiceOS overlays TalkBack-accessible
- ✅ Documentation: TalkBack user guide
- ✅ Automated accessibility tests

---

### **Phase 2: iOS VoiceOver (2 weeks)**

**Week 1:**
- [ ] Task 2.1: Implement iOS overlays with VoiceOver support
- [ ] Task 2.2: Add Swift accessibility extensions
- [ ] Task 2.3: Test with VoiceOver enabled
- [ ] Task 2.4: Custom rotor support

**Week 2:**
- [ ] Task 2.5: Dynamic type support (font scaling)
- [ ] Task 2.6: Reduce motion support
- [ ] Task 2.7: VoiceOver gestures customization
- [ ] Task 2.8: User testing with blind iOS users

**Deliverables:**
- ✅ iOS VoiceOS fully VoiceOver-accessible
- ✅ Documentation: VoiceOver user guide
- ✅ Automated accessibility tests

---

### **Phase 3: Desktop (3 weeks)**

**Week 1: Windows (NVDA/JAWS/Narrator)**
- [ ] Task 3.1: Compose Desktop UI Automation integration
- [ ] Task 3.2: Test with NVDA
- [ ] Task 3.3: Test with JAWS
- [ ] Task 3.4: Test with Narrator

**Week 2: macOS (VoiceOver)**
- [ ] Task 3.5: NSAccessibility integration
- [ ] Task 3.6: Test with macOS VoiceOver
- [ ] Task 3.7: Keyboard navigation (macOS)

**Week 3: Linux (Orca)**
- [ ] Task 3.8: ATK/AT-SPI integration
- [ ] Task 3.9: Test with Orca
- [ ] Task 3.10: Cross-platform testing

**Deliverables:**
- ✅ Desktop VoiceOS accessible on all platforms
- ✅ Documentation: Platform-specific guides
- ✅ Automated tests (all platforms)

---

### **Phase 4: Web (2 weeks)**

**Week 1:**
- [ ] Task 4.1: Implement ARIA attributes in React components
- [ ] Task 4.2: Keyboard navigation (Tab, Arrow keys)
- [ ] Task 4.3: Focus management
- [ ] Task 4.4: Live regions for announcements

**Week 2:**
- [ ] Task 4.5: Test with NVDA (Windows + Chrome)
- [ ] Task 4.6: Test with JAWS (Windows + Chrome)
- [ ] Task 4.7: Test with VoiceOver (macOS + Safari)
- [ ] Task 4.8: Test with Orca (Linux + Firefox)
- [ ] Task 4.9: axe DevTools audit
- [ ] Task 4.10: WAVE accessibility audit

**Deliverables:**
- ✅ WebAvanue fully WCAG 2.1 Level AA compliant
- ✅ Documentation: Web accessibility guide
- ✅ Automated ARIA linting (axe-core)

---

### **Phase 5: Testing & Compliance (2 weeks)**

**Week 1: Automated Testing**
- [ ] Task 5.1: Integrate accessibility testing in CI/CD
- [ ] Task 5.2: Android: Espresso accessibility tests
- [ ] Task 5.3: iOS: XCUITest accessibility tests
- [ ] Task 5.4: Web: axe-core + jest-axe tests
- [ ] Task 5.5: Desktop: Compose UI testing

**Week 2: Manual Testing & Compliance**
- [ ] Task 5.6: WCAG 2.1 Level AA audit (all platforms)
- [ ] Task 5.7: User testing with blind/low-vision users (5+ participants per platform)
- [ ] Task 5.8: Fix identified issues
- [ ] Task 5.9: Generate VPAT (Voluntary Product Accessibility Template)
- [ ] Task 5.10: Accessibility statement publication

**Deliverables:**
- ✅ WCAG 2.1 Level AA certification
- ✅ VPAT document
- ✅ Accessibility statement
- ✅ User testing report

---

## Code Examples

### **Example 1: Unified Accessibility API (MagicUI)**

```kotlin
// commonMain/AccessibilitySemantics.kt
package com.augmentalis.magicui.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Unified accessibility semantics for all platforms
 */
data class MagicAccessibility(
    val label: String,
    val hint: String? = null,
    val role: AccessibilityRole,
    val state: AccessibilityState = AccessibilityState(),
    val liveRegion: LiveRegionMode = LiveRegionMode.None
)

enum class AccessibilityRole {
    BUTTON, CHECKBOX, RADIO_BUTTON, TEXT_FIELD,
    IMAGE, HEADING, LINK, LIST, LIST_ITEM, DIALOG
}

data class AccessibilityState(
    val isEnabled: Boolean = true,
    val isSelected: Boolean = false,
    val isChecked: Boolean? = null,
    val value: String? = null
)

enum class LiveRegionMode {
    None, Polite, Assertive
}

/**
 * Apply accessibility semantics (platform-specific implementation)
 */
@Composable
expect fun Modifier.magicAccessibility(
    semantics: MagicAccessibility
): Modifier
```

```kotlin
// androidMain/AccessibilitySemantics.android.kt
actual fun Modifier.magicAccessibility(
    semantics: MagicAccessibility
): Modifier = this.semantics {
    contentDescription = semantics.label
    semantics.hint?.let { /* add hint */ }

    role = when (semantics.role) {
        AccessibilityRole.BUTTON -> Role.Button
        AccessibilityRole.CHECKBOX -> Role.Checkbox
        // ... map all roles
        else -> Role.Button
    }

    when (semantics.liveRegion) {
        LiveRegionMode.Polite -> liveRegion = LiveRegionMode.Polite
        LiveRegionMode.Assertive -> liveRegion = LiveRegionMode.Assertive
        else -> {}
    }

    if (!semantics.state.isEnabled) disabled()
    if (semantics.state.isSelected) selected = true
}
```

```swift
// iosMain/AccessibilitySemantics.ios.kt
actual fun Modifier.magicAccessibility(
    semantics: MagicAccessibility
): Modifier {
    // Implementation via Swift interop
    // Maps to SwiftUI .accessibilityLabel(), .accessibilityHint(), etc.
}
```

### **Example 2: VoiceOS + Screen Reader Integration**

```kotlin
// VoiceOS overlay with dual accessibility (voice + screen reader)
@Composable
fun DualAccessibleOverlay(elements: List<UIElement>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // VoiceOS VUID registration
            .withVUID(
                name = "selection_overlay",
                type = "overlay",
                aliases = listOf("selection", "numbered overlay")
            )
            // Screen reader semantics
            .magicAccessibility(
                MagicAccessibility(
                    label = "Voice selection overlay with ${elements.size} numbered elements",
                    hint = "Say 'select' followed by a number, or swipe to explore elements",
                    role = AccessibilityRole.DIALOG,
                    liveRegion = LiveRegionMode.Polite
                )
            )
    ) {
        elements.forEachIndexed { index, element ->
            NumberBadge(
                number = index + 1,
                element = element,
                // Dual accessibility
                modifier = Modifier
                    .withVUID(
                        name = "element_${index + 1}",
                        type = "selectable_element"
                    )
                    .magicAccessibility(
                        MagicAccessibility(
                            label = "Element ${index + 1}: ${element.text ?: "unlabeled"}",
                            hint = "Say 'select ${index + 1}' or double-tap",
                            role = AccessibilityRole.BUTTON
                        )
                    )
            )
        }
    }
}
```

**Result:**
- ✅ VoiceOS users: "Select 1" works
- ✅ TalkBack users: Swipe + double-tap works
- ✅ VoiceOver users: Swipe + double-tap works
- ✅ Combined: Voice command + screen reader feedback

---

## Testing Strategy

### **Automated Testing**

#### **Android: Espresso Accessibility**

```kotlin
@Test
fun testNumberedOverlayAccessibility() {
    // Enable TalkBack programmatically
    val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)

    // Launch overlay
    composeTestRule.setContent {
        NumberedSelectionOverlay(testElements)
    }

    // Test content descriptions exist
    composeTestRule.onNode(hasContentDescription("Voice selection overlay"))
        .assertExists()

    // Test all elements are accessible
    testElements.forEachIndexed { index, element ->
        composeTestRule
            .onNode(hasContentDescription("Element ${index + 1}"))
            .assertExists()
            .assertHasClickAction()
    }

    // Test keyboard navigation
    composeTestRule
        .onNode(hasContentDescription("Element 1"))
        .performKeyInput { pressKey(Key.Tab) }

    composeTestRule
        .onNode(hasContentDescription("Element 2"))
        .assertIsFocused()
}
```

#### **iOS: XCUITest Accessibility**

```swift
func testNumberedOverlayAccessibility() {
    let app = XCUIApplication()
    app.launch()

    // Enable VoiceOver
    XCUIDevice.shared.siriService.activate(text: "Turn on VoiceOver")

    // Find overlay
    let overlay = app.otherElements["Voice selection overlay"]
    XCTAssertTrue(overlay.exists)

    // Test all elements accessible
    for i in 1...5 {
        let badge = app.buttons["Element \(i)"]
        XCTAssertTrue(badge.exists)
        XCTAssertTrue(badge.isHittable)
    }

    // Test VoiceOver navigation
    let element1 = app.buttons["Element 1"]
    element1.swipeRight()  // VoiceOver next element

    let element2 = app.buttons["Element 2"]
    XCTAssertTrue(element2.hasFocus)
}
```

#### **Web: axe-core + jest-axe**

```typescript
// NumberedOverlay.test.tsx
import { render } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';

expect.extend(toHaveNoViolations);

describe('NumberedSelectionOverlay Accessibility', () => {
  it('should have no accessibility violations', async () => {
    const { container } = render(
      <NumberedSelectionOverlay elements={testElements} onSelect={jest.fn()} />
    );

    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });

  it('should have proper ARIA attributes', () => {
    const { getByRole } = render(
      <NumberedSelectionOverlay elements={testElements} onSelect={jest.fn()} />
    );

    const overlay = getByRole('dialog');
    expect(overlay).toHaveAttribute('aria-label', 'Voice selection overlay');

    const badges = getAllByRole('button');
    expect(badges).toHaveLength(testElements.length);
    badges.forEach((badge, index) => {
      expect(badge).toHaveAttribute('aria-label', expect.stringContaining(`Element ${index + 1}`));
    });
  });

  it('should be keyboard navigable', () => {
    const { getAllByRole } = render(
      <NumberedSelectionOverlay elements={testElements} onSelect={jest.fn()} />
    );

    const badges = getAllByRole('button');

    // Tab to first badge
    badges[0].focus();
    expect(badges[0]).toHaveFocus();

    // Tab to second badge
    fireEvent.keyDown(badges[0], { key: 'Tab' });
    expect(badges[1]).toHaveFocus();
  });
});
```

### **Manual Testing Checklist**

#### **TalkBack (Android)**

- [ ] Enable TalkBack (Settings → Accessibility → TalkBack)
- [ ] Launch VoiceOS
- [ ] Swipe through all overlays
- [ ] Verify content descriptions are clear
- [ ] Test double-tap activation
- [ ] Test custom actions menu
- [ ] Test live region announcements
- [ ] Disable TalkBack (verify app still works)

#### **VoiceOver (iOS)**

- [ ] Enable VoiceOver (Settings → Accessibility → VoiceOver)
- [ ] Launch VoiceOS
- [ ] Swipe through all overlays
- [ ] Verify labels and hints
- [ ] Test double-tap activation
- [ ] Test rotor navigation
- [ ] Test with reduced motion enabled
- [ ] Disable VoiceOver (verify app still works)

#### **NVDA (Windows)**

- [ ] Install NVDA (free, open-source)
- [ ] Launch VoiceOS desktop app
- [ ] Navigate with Tab key
- [ ] Test arrow key navigation
- [ ] Verify NVDA announcements
- [ ] Test with browse mode
- [ ] Test with forms mode

#### **Orca (Linux)**

- [ ] Enable Orca (gnome-control-center → Universal Access)
- [ ] Launch VoiceOS desktop app
- [ ] Navigate with Tab key
- [ ] Verify Orca announcements
- [ ] Test with flat review mode

---

## WCAG Compliance

### **WCAG 2.1 Level AA Requirements**

| Criterion | Requirement | Implementation |
|-----------|-------------|----------------|
| **1.1.1 Non-text Content** | All non-text content has text alternative | ✅ contentDescription on all images |
| **1.3.1 Info and Relationships** | Structure conveyed programmatically | ✅ Semantic roles (Button, Heading, etc.) |
| **1.4.3 Contrast** | 4.5:1 minimum contrast ratio | ✅ MagicUI theme ensures compliant colors |
| **2.1.1 Keyboard** | All functionality via keyboard | ✅ Tab navigation, Enter/Space activation |
| **2.4.3 Focus Order** | Logical focus order | ✅ Composable order matches visual order |
| **2.4.7 Focus Visible** | Visible focus indicator | ✅ Material3 default focus ring |
| **3.2.4 Consistent Identification** | Consistent labeling | ✅ "Submit button" always labeled same |
| **4.1.2 Name, Role, Value** | Programmatic name/role/value | ✅ Semantics provide all three |
| **4.1.3 Status Messages** | Status changes announced | ✅ Live regions for dynamic content |

### **Compliance Checklist**

**Perceivable:**
- [ ] All images have alt text (contentDescription)
- [ ] Color is not the only visual means of conveying information
- [ ] Content can be presented in different ways (portrait/landscape)
- [ ] Content is distinguishable (sufficient contrast)

**Operable:**
- [ ] All functionality available from keyboard
- [ ] Users have enough time to read and use content
- [ ] Content does not cause seizures (no flashing >3 times/sec)
- [ ] Users can easily navigate and find content
- [ ] Multiple ways to find pages (search, sitemap, breadcrumbs)

**Understandable:**
- [ ] Text is readable and understandable
- [ ] Content appears and operates in predictable ways
- [ ] Users are helped to avoid and correct mistakes

**Robust:**
- [ ] Content is compatible with current and future assistive technologies
- [ ] Valid HTML/ARIA (web)
- [ ] Name, role, value available for all components

---

## Timeline & Resources

### **Timeline (11 weeks)**

```
Week 1-2:   Android TalkBack
Week 3-4:   iOS VoiceOver
Week 5-7:   Desktop (Windows/macOS/Linux)
Week 8-9:   Web ARIA
Week 10-11: Testing & Compliance
```

### **Resources Required**

**Personnel:**
- 1x Android developer (2 weeks)
- 1x iOS developer (2 weeks)
- 1x Desktop developer (3 weeks)
- 1x Web developer (2 weeks)
- 1x QA engineer (2 weeks testing)
- 1x Accessibility consultant (ongoing review)

**Tools:**
- **Android:** Android Accessibility Scanner, Espresso
- **iOS:** Accessibility Inspector, XCUITest
- **Web:** axe DevTools, WAVE, Lighthouse
- **Desktop:** NVDA, JAWS (trial), Orca

**Budget:**
- JAWS license (for testing): $1,095/year
- Accessibility consultant: $5,000-10,000
- User testing (blind/low-vision): $2,000-5,000
- Total: ~$10,000-20,000

---

## Success Metrics

| Metric | Target | Current | Gap |
|--------|--------|---------|-----|
| WCAG 2.1 Level AA | 100% | 40% (estimated) | 60% |
| TalkBack compatibility | 100% | 0% | 100% |
| VoiceOver compatibility | 100% | 0% | 100% |
| Desktop SR compatibility | 100% | 0% | 100% |
| Keyboard navigation | 100% | 60% (estimated) | 40% |
| User satisfaction (blind users) | >90% | N/A | N/A |

---

## Conclusion

This plan provides comprehensive screen reader support for VoiceOS and MagicUI across all platforms:

✅ **Android:** TalkBack support via Compose semantics
✅ **iOS:** VoiceOver support via SwiftUI accessibility
✅ **Desktop:** NVDA/JAWS/VoiceOver/Orca support via platform APIs
✅ **Web:** ARIA support for all major screen readers

**Key Benefits:**
1. Makes VoiceOS accessible to 2.2 billion people with visual impairments
2. WCAG 2.1 Level AA compliance (legal requirement in many countries)
3. Better UX for all users (keyboard navigation, clear labels, etc.)
4. Competitive advantage (most voice control apps lack screen reader support)

**Timeline:** 11 weeks (2.75 months)
**Effort:** 10-12 person-weeks
**Budget:** $10,000-20,000

---

**Next Steps:**
1. Review and approve this plan
2. Allocate resources (developers, QA, budget)
3. Begin Phase 1 (Android TalkBack)
4. Parallel: Start MagicUI unified accessibility API
