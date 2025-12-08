# AvaCode Target Framework Mappings

**Comprehensive Design Document for Code Generation**

Version: 1.0.0
Last Updated: 2025-10-28

---

## Table of Contents

1. [Overview](#overview)
2. [Built-in Components Summary](#built-in-components-summary)
3. [Complete Component Mapping Table](#complete-component-mapping-table)
4. [State Management Patterns](#state-management-patterns)
5. [Callback/Event Handler Patterns](#callback-event-handler-patterns)
6. [Import Generation Templates](#import-generation-templates)
7. [Code Structure Templates](#code-structure-templates)
8. [Full Working Examples](#full-working-examples)
9. [Type Conversion Reference](#type-conversion-reference)
10. [Code Generation Recommendations](#code-generation-recommendations)

---

## Overview

AvaCode generates native code for three target platforms:
- **Kotlin Jetpack Compose** (Android + Desktop JVM)
- **SwiftUI** (iOS)
- **React/TypeScript** (Web - future support)

This document provides complete mappings from AvaCode DSL to each target platform.

### Architecture Pattern

All platforms follow the **expect/actual** pattern from the existing codebase:
- `commonMain/`: Shared business logic and models
- `androidMain/`: Android-specific Compose implementations
- `iosMain/`: iOS-specific SwiftUI implementations (via Kotlin/Native)
- `jsMain/`: Web-specific React implementations (future)

---

## Built-in Components Summary

From `/runtime/libraries/AvaUI/src/commonMain/kotlin/.../BuiltInComponents.kt`:

### 1. ColorPicker
**Type**: `ColorPicker`
**Category**: INPUT
**Supports Children**: No

**Properties**:
- `id` (STRING, optional) - Unique component identifier
- `initialColor` (COLOR, optional, default: "#FFFFFF") - Initial selected color
- `mode` (ENUM, optional, default: "FULL") - Display mode
  - Values: `FULL`, `COMPACT`, `PRESETS_ONLY`, `HEX_ONLY`, `WHEEL`, `HSV_SLIDERS`, `RGB_SLIDERS`, `DESIGNER`
- `showAlpha` (BOOLEAN, optional, default: true) - Show alpha channel control

**Callbacks**:
- `onColorChanged(color: COLOR)` - Called when color changes
- `onConfirm(color: COLOR)` - Called when user confirms color
- `onCancel()` - Called when user cancels

### 2. Preferences
**Type**: `Preferences`
**Category**: GENERAL
**Supports Children**: No

**Properties**: None
**Callbacks**: None

Note: This is a storage component accessed via `PreferenceStore` API, not a UI component.

### 3. Text
**Type**: `Text`
**Category**: DISPLAY
**Supports Children**: No

**Properties**:
- `text` (STRING, required) - Text content
- `size` (FLOAT, optional, default: 16f) - Font size in sp
- `color` (COLOR, optional, default: "#000000") - Text color

**Callbacks**: None

### 4. Button
**Type**: `Button`
**Category**: INPUT
**Supports Children**: No

**Properties**:
- `text` (STRING, required) - Button text
- `enabled` (BOOLEAN, optional, default: true) - Button enabled state

**Callbacks**:
- `onClick()` - Called when button clicked

### 5. Container
**Type**: `Container`
**Category**: CONTAINER
**Supports Children**: Yes

**Properties**:
- `orientation` (ENUM, optional, default: "vertical") - Layout orientation
  - Values: `vertical`, `horizontal`

**Callbacks**: None

---

## Complete Component Mapping Table

### 1. ColorPicker Component

#### DSL Syntax
```kotlin
ColorPicker {
    id = "colorPicker1"
    initialColor = "#FF5722"
    mode = "FULL"
    showAlpha = true

    onColorChanged = { color ->
        println("Color changed: $color")
    }

    onConfirm = { color ->
        println("Confirmed: $color")
    }

    onCancel = {
        println("Cancelled")
    }
}
```

#### Kotlin Compose Code
```kotlin
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.augmentalis.voiceos.colorpicker.*

@Composable
fun ColorPickerComponent() {
    var selectedColor by remember { mutableStateOf(Color(0xFFFF5722)) }
    var showPicker by remember { mutableStateOf(false) }

    val picker = remember {
        ColorPickerFactory.create(
            initialColor = ColorRGBA.fromHexString("#FF5722"),
            config = ColorPickerConfig(
                mode = ColorPickerMode.FULL,
                showAlpha = true
            )
        ).apply {
            onColorChanged = { color ->
                selectedColor = Color(color.toARGBInt())
                println("Color changed: $color")
            }

            onConfirmed = { color ->
                selectedColor = Color(color.toARGBInt())
                println("Confirmed: $color")
                showPicker = false
            }

            onCancelled = {
                println("Cancelled")
                showPicker = false
            }
        }
    }

    LaunchedEffect(showPicker) {
        if (showPicker) {
            picker.show()
        } else {
            picker.hide()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            picker.dispose()
        }
    }

    // UI to trigger picker
    Button(onClick = { showPicker = true }) {
        Text("Pick Color")
    }
}
```

#### SwiftUI Code
```swift
import SwiftUI
import Foundation

struct ColorPickerComponent: View {
    @State private var selectedColor: Color = Color(hex: "#FF5722")
    @State private var showPicker = false

    var body: some View {
        VStack {
            Button("Pick Color") {
                showPicker = true
            }
            .sheet(isPresented: $showPicker) {
                ColorPickerView(
                    selectedColor: $selectedColor,
                    mode: .full,
                    showAlpha: true,
                    onConfirm: { color in
                        print("Confirmed: \(color.toHex())")
                        showPicker = false
                    },
                    onCancel: {
                        print("Cancelled")
                        showPicker = false
                    }
                )
            }
        }
    }
}

// Helper extension
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 6: // RGB
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }

    func toHex() -> String {
        guard let components = self.cgColor?.components else { return "#000000" }
        let r = Int(components[0] * 255)
        let g = Int(components[1] * 255)
        let b = Int(components[2] * 255)
        return String(format: "#%02X%02X%02X", r, g, b)
    }
}
```

#### React/TypeScript Code (Future)
```typescript
import React, { useState } from 'react';
import { ChromePicker, ColorResult } from 'react-color';

interface ColorPickerProps {
  id?: string;
  initialColor?: string;
  mode?: 'full' | 'compact' | 'presets';
  showAlpha?: boolean;
  onColorChanged?: (color: string) => void;
  onConfirm?: (color: string) => void;
  onCancel?: () => void;
}

export const ColorPickerComponent: React.FC<ColorPickerProps> = ({
  id = 'colorPicker1',
  initialColor = '#FF5722',
  mode = 'full',
  showAlpha = true,
  onColorChanged,
  onConfirm,
  onCancel,
}) => {
  const [color, setColor] = useState(initialColor);
  const [showPicker, setShowPicker] = useState(false);

  const handleColorChange = (color: ColorResult) => {
    const hexColor = color.hex;
    setColor(hexColor);
    onColorChanged?.(hexColor);
  };

  const handleConfirm = () => {
    onConfirm?.(color);
    setShowPicker(false);
  };

  const handleCancel = () => {
    onCancel?.();
    setShowPicker(false);
  };

  return (
    <div className="color-picker-container">
      <button onClick={() => setShowPicker(true)}>
        Pick Color
      </button>

      {showPicker && (
        <div className="color-picker-modal">
          <ChromePicker
            color={color}
            onChange={handleColorChange}
            disableAlpha={!showAlpha}
          />
          <div className="color-picker-actions">
            <button onClick={handleConfirm}>OK</button>
            <button onClick={handleCancel}>Cancel</button>
          </div>
        </div>
      )}
    </div>
  );
};
```

---

### 2. Text Component

#### DSL Syntax
```kotlin
Text {
    text = "Hello, World!"
    size = 24f
    color = "#007AFF"
}
```

#### Kotlin Compose Code
```kotlin
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

Text(
    text = "Hello, World!",
    fontSize = 24.sp,
    color = Color(0xFF007AFF)
)
```

#### SwiftUI Code
```swift
Text("Hello, World!")
    .font(.system(size: 24))
    .foregroundColor(Color(hex: "#007AFF"))
```

#### React/TypeScript Code
```typescript
<span
  style={{
    fontSize: '24px',
    color: '#007AFF'
  }}
>
  Hello, World!
</span>
```

---

### 3. Button Component

#### DSL Syntax
```kotlin
Button {
    text = "Click Me"
    enabled = true

    onClick = {
        println("Button clicked!")
    }
}
```

#### Kotlin Compose Code
```kotlin
import androidx.compose.material3.Button
import androidx.compose.material3.Text

Button(
    onClick = {
        println("Button clicked!")
    },
    enabled = true
) {
    Text("Click Me")
}
```

#### SwiftUI Code
```swift
Button(action: {
    print("Button clicked!")
}) {
    Text("Click Me")
}
.disabled(false)
```

#### React/TypeScript Code
```typescript
<button
  onClick={() => {
    console.log('Button clicked!');
  }}
  disabled={false}
>
  Click Me
</button>
```

---

### 4. Container Component

#### DSL Syntax
```kotlin
Container {
    orientation = "vertical"

    children = listOf(
        Text { text = "Item 1" },
        Text { text = "Item 2" },
        Text { text = "Item 3" }
    )
}
```

#### Kotlin Compose Code
```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text

// For vertical orientation
Column {
    Text("Item 1")
    Text("Item 2")
    Text("Item 3")
}

// For horizontal orientation
Row {
    Text("Item 1")
    Text("Item 2")
    Text("Item 3")
}
```

#### SwiftUI Code
```swift
// For vertical orientation
VStack {
    Text("Item 1")
    Text("Item 2")
    Text("Item 3")
}

// For horizontal orientation
HStack {
    Text("Item 1")
    Text("Item 2")
    Text("Item 3")
}
```

#### React/TypeScript Code
```typescript
// For vertical orientation
<div style={{ display: 'flex', flexDirection: 'column' }}>
  <span>Item 1</span>
  <span>Item 2</span>
  <span>Item 3</span>
</div>

// For horizontal orientation
<div style={{ display: 'flex', flexDirection: 'row' }}>
  <span>Item 1</span>
  <span>Item 2</span>
  <span>Item 3</span>
</div>
```

---

### 5. Preferences Component

#### DSL Syntax
```kotlin
// Preferences is an API component, not UI
val prefs = Preferences {
    preferenceName = "app_settings"
    encrypted = false
}

// Usage
prefs.putString("theme", "dark")
val theme = prefs.getString("theme", "light")
```

#### Kotlin Compose Code
```kotlin
import com.augmentalis.voiceos.preferences.*

// Initialize (typically in Application class)
// PreferenceStoreFactory.initialize(context)

// Use in Composable
@Composable
fun PreferencesExample() {
    val prefs = remember {
        PreferenceStoreFactory.create(
            PreferenceConfig(
                preferenceName = "app_settings",
                encrypted = false
            )
        )
    }

    var theme by remember {
        mutableStateOf(
            prefs.getString("theme", "light").getOrDefault("light")
        )
    }

    Button(onClick = {
        val newTheme = if (theme == "light") "dark" else "light"
        prefs.putString("theme", newTheme)
        theme = newTheme
    }) {
        Text("Toggle Theme: $theme")
    }
}
```

#### SwiftUI Code
```swift
import SwiftUI

struct PreferencesExample: View {
    @AppStorage("theme") private var theme = "light"

    var body: some View {
        Button("Toggle Theme: \(theme)") {
            theme = (theme == "light") ? "dark" : "light"
        }
    }
}
```

#### React/TypeScript Code
```typescript
import React, { useState, useEffect } from 'react';

export const PreferencesExample: React.FC = () => {
  const [theme, setTheme] = useState(() => {
    return localStorage.getItem('theme') || 'light';
  });

  useEffect(() => {
    localStorage.setItem('theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme(theme === 'light' ? 'dark' : 'light');
  };

  return (
    <button onClick={toggleTheme}>
      Toggle Theme: {theme}
    </button>
  );
};
```

---

## State Management Patterns

### Kotlin Compose State Management

#### Basic State
```kotlin
// Simple mutable state
var count by remember { mutableStateOf(0) }

// State with initial value from prop
var text by remember { mutableStateOf(initialText) }

// State list
var items by remember { mutableStateOf(listOf<String>()) }
```

#### Derived State
```kotlin
// Computed from other state
val isValid by remember {
    derivedStateOf {
        text.isNotEmpty() && text.length >= 3
    }
}
```

#### State Hoisting Pattern
```kotlin
@Composable
fun StatefulComponent() {
    var value by remember { mutableStateOf("") }

    StatelessComponent(
        value = value,
        onValueChange = { value = it }
    )
}

@Composable
fun StatelessComponent(
    value: String,
    onValueChange: (String) -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange
    )
}
```

#### Side Effects
```kotlin
// Run once on mount
LaunchedEffect(Unit) {
    // Load data
}

// Run when key changes
LaunchedEffect(userId) {
    loadUserData(userId)
}

// Cleanup on dispose
DisposableEffect(Unit) {
    val listener = setupListener()
    onDispose {
        listener.cleanup()
    }
}
```

### SwiftUI State Management

#### Basic State
```swift
// Simple state
@State private var count = 0

// State with initial value
@State private var text: String = ""

// State array
@State private var items: [String] = []
```

#### Binding
```swift
// Create binding from state
@State private var text = ""
TextField("Enter text", text: $text)

// Custom binding
let binding = Binding(
    get: { self.text },
    set: { self.text = $0 }
)
```

#### Observed Objects
```swift
// For complex state
class ViewModel: ObservableObject {
    @Published var count = 0
    @Published var items: [String] = []
}

struct ContentView: View {
    @StateObject private var viewModel = ViewModel()

    var body: some View {
        Text("Count: \(viewModel.count)")
    }
}
```

#### Environment
```swift
// Share state across views
@EnvironmentObject var settings: AppSettings

// Environment values
@Environment(\.colorScheme) var colorScheme
```

#### Side Effects
```swift
// Run on appear
.onAppear {
    loadData()
}

// Run on disappear
.onDisappear {
    cleanup()
}

// Run when value changes
.onChange(of: searchText) { newValue in
    performSearch(newValue)
}

// Task for async work
.task {
    await loadUserData()
}
```

### React/TypeScript State Management

#### Basic State
```typescript
// Simple state
const [count, setCount] = useState(0);

// State with initial value
const [text, setText] = useState('');

// State array
const [items, setItems] = useState<string[]>([]);
```

#### Computed Values (useMemo)
```typescript
const isValid = useMemo(() => {
  return text.length >= 3 && text.length <= 20;
}, [text]);
```

#### Effects (useEffect)
```typescript
// Run once on mount
useEffect(() => {
  loadData();
}, []);

// Run when dependency changes
useEffect(() => {
  loadUserData(userId);
}, [userId]);

// Cleanup
useEffect(() => {
  const subscription = subscribe();
  return () => {
    subscription.unsubscribe();
  };
}, []);
```

#### Context for Global State
```typescript
const ThemeContext = React.createContext<{
  theme: string;
  setTheme: (theme: string) => void;
}>({
  theme: 'light',
  setTheme: () => {},
});

// Provider
export const ThemeProvider: React.FC = ({ children }) => {
  const [theme, setTheme] = useState('light');

  return (
    <ThemeContext.Provider value={{ theme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};

// Consumer
const { theme, setTheme } = useContext(ThemeContext);
```

---

## Callback/Event Handler Patterns

### Callback Signature Mappings

| DSL Signature | Kotlin Compose | SwiftUI | React/TypeScript |
|---------------|----------------|---------|------------------|
| `onClick: () => void` | `onClick: () -> Unit` | `action: () -> Void` | `onClick: () => void` |
| `onColorChanged: (color) => void` | `onColorChanged: (ColorRGBA) -> Unit` | `onColorChanged: (Color) -> Void` | `onColorChanged: (color: string) => void` |
| `onConfirm: (color) => void` | `onConfirmed: (ColorRGBA) -> Unit` | `onConfirm: (Color) -> Void` | `onConfirm: (color: string) => void` |
| `onCancel: () => void` | `onCancelled: () -> Unit` | `onCancel: () -> Void` | `onCancel: () => void` |
| `onTextChange: (text) => void` | `onValueChange: (String) -> Unit` | `onChange: (String) -> Void` | `onChange: (text: string) => void` |

### Kotlin Compose Callback Patterns

```kotlin
// Simple callback
Button(onClick = { /* action */ }) {
    Text("Click")
}

// Callback with parameter
ColorPicker(
    onColorChanged = { color ->
        // Handle color change
    }
)

// Lambda with receiver
LazyColumn {
    items(items) { item ->
        Text(item)
    }
}

// Trailing lambda
remember {
    // Computation
}
```

### SwiftUI Callback Patterns

```swift
// Simple action
Button(action: {
    // action
}) {
    Text("Click")
}

// Simplified syntax
Button("Click") {
    // action
}

// Closure with parameters
ColorPicker(
    selection: $selectedColor,
    label: { Text("Choose Color") }
)
.onChange(of: selectedColor) { newColor in
    // Handle change
}

// Trailing closure
VStack {
    // content
}
```

### React/TypeScript Callback Patterns

```typescript
// Arrow function (recommended)
<Button onClick={() => handleClick()}>
  Click
</Button>

// With event parameter
<input
  onChange={(e) => setText(e.target.value)}
/>

// Function reference
<Button onClick={handleClick}>
  Click
</Button>

// Callback with parameter
<ColorPicker
  onColorChanged={(color) => {
    setSelectedColor(color);
  }}
/>
```

---

## Import Generation Templates

### Kotlin Compose Imports

```kotlin
// Material Design 3
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold

// Layout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width

// State
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// UI
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Custom components
import com.augmentalis.voiceos.colorpicker.*
import com.augmentalis.voiceos.preferences.*
```

### SwiftUI Imports

```swift
import SwiftUI
import Foundation
import Combine // For advanced reactive patterns

// Platform-specific
#if os(iOS)
import UIKit
#elseif os(macOS)
import AppKit
#endif

// Custom components
// (In Swift, typically in same module or via package)
```

### React/TypeScript Imports

```typescript
// React core
import React, {
  useState,
  useEffect,
  useMemo,
  useCallback,
  useContext,
  useRef
} from 'react';

// React DOM
import ReactDOM from 'react-dom';

// Third-party UI
import { ChromePicker } from 'react-color';

// Custom components
import { ColorPicker } from './components/ColorPicker';
import { Button } from './components/Button';

// Styles
import './App.css';

// Types
import type { FC, ReactNode } from 'react';
```

---

## Code Structure Templates

### Kotlin Compose File Structure

```kotlin
package com.example.app

// Imports
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*

/**
 * [Component Name] - [Brief description]
 *
 * @param [paramName] [Description]
 */
@Composable
fun ComponentName(
    modifier: Modifier = Modifier,
    // Props
    text: String = "",
    enabled: Boolean = true,
    // Callbacks
    onClick: () -> Unit = {}
) {
    // State
    var internalState by remember { mutableStateOf(initialValue) }

    // Derived state
    val computedValue by remember {
        derivedStateOf {
            // Computation
        }
    }

    // Side effects
    LaunchedEffect(key) {
        // Effect
    }

    DisposableEffect(Unit) {
        onDispose {
            // Cleanup
        }
    }

    // UI
    Column(modifier = modifier) {
        // Content
    }
}

// Preview
@Preview(showBackground = true)
@Composable
fun ComponentNamePreview() {
    MaterialTheme {
        ComponentName()
    }
}
```

### SwiftUI File Structure

```swift
import SwiftUI

/// [Component Name] - [Brief description]
///
/// - Parameters:
///   - [paramName]: [Description]
struct ComponentName: View {
    // Props (from parent)
    let text: String
    let enabled: Bool
    var onTap: () -> Void

    // State (internal)
    @State private var internalState = initialValue

    // Computed properties
    private var computedValue: String {
        // Computation
        return result
    }

    var body: some View {
        VStack {
            // Content
        }
        .onAppear {
            // Setup
        }
        .onDisappear {
            // Cleanup
        }
    }
}

// Preview
#Preview {
    ComponentName(
        text: "Example",
        enabled: true,
        onTap: {}
    )
}
```

### React/TypeScript File Structure

```typescript
import React, { useState, useEffect } from 'react';

/**
 * [Component Name] - [Brief description]
 */
interface ComponentNameProps {
  /** [Description] */
  text: string;
  /** [Description] */
  enabled?: boolean;
  /** [Description] */
  onTap?: () => void;
}

/**
 * [Component Name] component
 */
export const ComponentName: React.FC<ComponentNameProps> = ({
  text,
  enabled = true,
  onTap,
}) => {
  // State
  const [internalState, setInternalState] = useState(initialValue);

  // Computed values
  const computedValue = useMemo(() => {
    // Computation
    return result;
  }, [dependencies]);

  // Effects
  useEffect(() => {
    // Setup

    return () => {
      // Cleanup
    };
  }, [dependencies]);

  // Event handlers
  const handleEvent = useCallback(() => {
    // Handler
  }, [dependencies]);

  // Render
  return (
    <div className="component-name">
      {/* Content */}
    </div>
  );
};

// Export default if single component
export default ComponentName;
```

---

## Full Working Examples

### Example 1: ColorPicker App

Complete application with color picker functionality.

#### Kotlin Compose (Android/Desktop)

```kotlin
package com.example.colorpickerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceos.colorpicker.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ColorPicker factory
        ColorPickerFactory.initialize(this)

        setContent {
            ColorPickerAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ColorPickerScreen()
                }
            }
        }
    }
}

@Composable
fun ColorPickerScreen() {
    var selectedColor by remember { mutableStateOf(Color.White) }
    var showPicker by remember { mutableStateOf(false) }
    var colorHistory by remember { mutableStateOf(listOf<Color>()) }

    val picker = remember {
        ColorPickerFactory.create(
            initialColor = ColorRGBA.WHITE,
            config = ColorPickerConfig.designer()
        ).apply {
            onColorChanged = { color ->
                selectedColor = Color(color.toARGBInt())
            }

            onConfirmed = { color ->
                selectedColor = Color(color.toARGBInt())
                colorHistory = (listOf(selectedColor) + colorHistory).take(10)
                showPicker = false
            }

            onCancelled = {
                showPicker = false
            }
        }
    }

    LaunchedEffect(showPicker) {
        if (showPicker) {
            picker.show()
        } else {
            picker.hide()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            picker.dispose()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Color Picker Demo",
            style = MaterialTheme.typography.headlineMedium
        )

        // Color preview box
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(selectedColor)
        )

        // Color hex display
        Text(
            text = "Selected: #${selectedColor.value.toString(16).uppercase().takeLast(6)}",
            style = MaterialTheme.typography.bodyLarge
        )

        // Pick color button
        Button(onClick = { showPicker = true }) {
            Text("Pick Color")
        }

        // Color history
        if (colorHistory.isNotEmpty()) {
            Text(
                text = "Recent Colors",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colorHistory.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(color)
                    )
                }
            }
        }
    }
}

@Composable
fun ColorPickerAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        content = content
    )
}

// Extension to convert ColorRGBA to Android Color int
fun ColorRGBA.toARGBInt(): Int {
    return android.graphics.Color.argb(alpha, red, green, blue)
}
```

#### SwiftUI (iOS)

```swift
import SwiftUI

@main
struct ColorPickerApp: App {
    var body: some Scene {
        WindowGroup {
            ColorPickerScreen()
        }
    }
}

struct ColorPickerScreen: View {
    @State private var selectedColor: Color = .white
    @State private var showPicker = false
    @State private var colorHistory: [Color] = []

    var body: some View {
        NavigationView {
            VStack(spacing: 16) {
                Text("Color Picker Demo")
                    .font(.largeTitle)
                    .padding(.top)

                // Color preview box
                Rectangle()
                    .fill(selectedColor)
                    .frame(width: 200, height: 200)
                    .cornerRadius(10)

                // Color hex display
                Text("Selected: \(selectedColor.toHex())")
                    .font(.body)

                // Pick color button
                Button("Pick Color") {
                    showPicker = true
                }
                .buttonStyle(.borderedProminent)
                .sheet(isPresented: $showPicker) {
                    ColorPicker(
                        "Choose Color",
                        selection: $selectedColor,
                        supportsOpacity: true
                    )
                    .presentationDetents([.medium])
                    .presentationDragIndicator(.visible)
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Done") {
                                colorHistory.insert(selectedColor, at: 0)
                                colorHistory = Array(colorHistory.prefix(10))
                                showPicker = false
                            }
                        }
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") {
                                showPicker = false
                            }
                        }
                    }
                }

                // Color history
                if !colorHistory.isEmpty {
                    Text("Recent Colors")
                        .font(.headline)
                        .padding(.top)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(0..<colorHistory.count, id: \.self) { index in
                                Rectangle()
                                    .fill(colorHistory[index])
                                    .frame(width: 40, height: 40)
                                    .cornerRadius(5)
                                    .onTapGesture {
                                        selectedColor = colorHistory[index]
                                    }
                            }
                        }
                        .padding(.horizontal)
                    }
                }

                Spacer()
            }
            .padding()
            .navigationBarHidden(true)
        }
    }
}

// Color extension helpers
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 6: // RGB
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }

    func toHex() -> String {
        guard let components = self.cgColor?.components else { return "#FFFFFF" }
        let r = Int(components[0] * 255)
        let g = Int(components[1] * 255)
        let b = Int(components[2] * 255)
        return String(format: "#%02X%02X%02X", r, g, b)
    }
}

#Preview {
    ColorPickerScreen()
}
```

#### React/TypeScript (Web)

```typescript
// App.tsx
import React, { useState, useCallback } from 'react';
import { ChromePicker, ColorResult } from 'react-color';
import './App.css';

interface ColorHistory {
  hex: string;
  rgb: { r: number; g: number; b: number; a: number };
}

export const ColorPickerApp: React.FC = () => {
  const [selectedColor, setSelectedColor] = useState('#FFFFFF');
  const [showPicker, setShowPicker] = useState(false);
  const [colorHistory, setColorHistory] = useState<ColorHistory[]>([]);

  const handleColorChange = useCallback((color: ColorResult) => {
    setSelectedColor(color.hex);
  }, []);

  const handleConfirm = useCallback(() => {
    const newHistory: ColorHistory = {
      hex: selectedColor,
      rgb: { r: 0, g: 0, b: 0, a: 1 } // Would extract from color
    };
    setColorHistory(prev => [newHistory, ...prev].slice(0, 10));
    setShowPicker(false);
  }, [selectedColor]);

  const handleCancel = useCallback(() => {
    setShowPicker(false);
  }, []);

  return (
    <div className="app">
      <div className="container">
        <h1>Color Picker Demo</h1>

        {/* Color preview box */}
        <div
          className="color-preview"
          style={{ backgroundColor: selectedColor }}
        />

        {/* Color hex display */}
        <p className="color-text">Selected: {selectedColor}</p>

        {/* Pick color button */}
        <button
          className="pick-button"
          onClick={() => setShowPicker(true)}
        >
          Pick Color
        </button>

        {/* Color picker modal */}
        {showPicker && (
          <div className="picker-modal">
            <div className="picker-backdrop" onClick={handleCancel} />
            <div className="picker-content">
              <ChromePicker
                color={selectedColor}
                onChange={handleColorChange}
              />
              <div className="picker-actions">
                <button onClick={handleConfirm}>OK</button>
                <button onClick={handleCancel}>Cancel</button>
              </div>
            </div>
          </div>
        )}

        {/* Color history */}
        {colorHistory.length > 0 && (
          <div className="history">
            <h3>Recent Colors</h3>
            <div className="history-grid">
              {colorHistory.map((color, index) => (
                <div
                  key={index}
                  className="history-item"
                  style={{ backgroundColor: color.hex }}
                  onClick={() => setSelectedColor(color.hex)}
                />
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default ColorPickerApp;
```

```css
/* App.css */
.app {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
}

.container {
  background: white;
  padding: 32px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  max-width: 500px;
  width: 100%;
}

h1 {
  text-align: center;
  margin-bottom: 24px;
  color: #333;
}

.color-preview {
  width: 200px;
  height: 200px;
  margin: 0 auto 16px;
  border-radius: 8px;
  border: 1px solid #ddd;
}

.color-text {
  text-align: center;
  font-size: 18px;
  margin-bottom: 16px;
  color: #666;
}

.pick-button {
  display: block;
  margin: 0 auto 24px;
  padding: 12px 24px;
  background-color: #007AFF;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.pick-button:hover {
  background-color: #0051D5;
}

.picker-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
}

.picker-backdrop {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
}

.picker-content {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.picker-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

.picker-actions button {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.picker-actions button:first-child {
  background-color: #007AFF;
  color: white;
}

.picker-actions button:last-child {
  background-color: #f0f0f0;
  color: #333;
}

.history {
  margin-top: 24px;
}

.history h3 {
  text-align: center;
  margin-bottom: 12px;
  color: #333;
}

.history-grid {
  display: flex;
  gap: 8px;
  justify-content: center;
  flex-wrap: wrap;
}

.history-item {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  border: 1px solid #ddd;
  cursor: pointer;
  transition: transform 0.2s;
}

.history-item:hover {
  transform: scale(1.1);
}
```

---

## Type Conversion Reference

### Property Type Mappings

| DSL Type | Kotlin Type | Swift Type | TypeScript Type |
|----------|-------------|------------|-----------------|
| STRING | `String` | `String` | `string` |
| INT | `Int` | `Int` | `number` |
| FLOAT | `Float` | `Double` | `number` |
| BOOLEAN | `Boolean` | `Bool` | `boolean` |
| COLOR | `Color` (Compose) / `ColorRGBA` | `Color` | `string` (hex) |
| ENUM | `enum class` | `enum` | `union type` |

### Color Type Conversions

#### Kotlin Compose
```kotlin
// Hex string to Color
val color = Color(0xFFFF5722)

// ColorRGBA to Compose Color
val composeColor = Color(colorRGBA.toARGBInt())

// Color to hex string
val hex = "#${color.value.toString(16).uppercase().takeLast(6)}"
```

#### SwiftUI
```swift
// Hex string to Color
let color = Color(hex: "#FF5722")

// Color to hex string
let hex = color.toHex()

// RGB to Color
let color = Color(
    red: Double(r) / 255.0,
    green: Double(g) / 255.0,
    blue: Double(b) / 255.0
)
```

#### React/TypeScript
```typescript
// Hex string (native format)
const color = '#FF5722';

// RGB to hex
const rgbToHex = (r: number, g: number, b: number) => {
  return `#${[r, g, b].map(x => {
    const hex = x.toString(16);
    return hex.length === 1 ? '0' + hex : hex;
  }).join('')}`;
};

// Hex to RGB
const hexToRgb = (hex: string) => {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  return result ? {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16)
  } : null;
};
```

### Enum Type Conversions

#### Kotlin
```kotlin
enum class ColorPickerMode {
    FULL,
    COMPACT,
    PRESETS_ONLY,
    HEX_ONLY,
    WHEEL,
    HSV_SLIDERS,
    RGB_SLIDERS
}

// String to enum
val mode = ColorPickerMode.valueOf("FULL")

// Enum to string
val modeStr = mode.name
```

#### Swift
```swift
enum ColorPickerMode: String, CaseIterable {
    case full = "FULL"
    case compact = "COMPACT"
    case presetsOnly = "PRESETS_ONLY"
    case hexOnly = "HEX_ONLY"
    case wheel = "WHEEL"
    case hsvSliders = "HSV_SLIDERS"
    case rgbSliders = "RGB_SLIDERS"
}

// String to enum
let mode = ColorPickerMode(rawValue: "FULL")

// Enum to string
let modeStr = mode.rawValue
```

#### TypeScript
```typescript
type ColorPickerMode =
  | 'FULL'
  | 'COMPACT'
  | 'PRESETS_ONLY'
  | 'HEX_ONLY'
  | 'WHEEL'
  | 'HSV_SLIDERS'
  | 'RGB_SLIDERS';

// Runtime validation
const isValidMode = (mode: string): mode is ColorPickerMode => {
  return ['FULL', 'COMPACT', 'PRESETS_ONLY', 'HEX_ONLY',
          'WHEEL', 'HSV_SLIDERS', 'RGB_SLIDERS'].includes(mode);
};
```

---

## Code Generation Recommendations

### 1. Architecture Approach

**Recommended: Template-Based Code Generation**

Use template engines with target-specific templates:

```
codegen/
├── templates/
│   ├── kotlin/
│   │   ├── component.kt.template
│   │   ├── screen.kt.template
│   │   └── imports.kt.template
│   ├── swift/
│   │   ├── component.swift.template
│   │   ├── screen.swift.template
│   │   └── imports.swift.template
│   └── typescript/
│       ├── component.tsx.template
│       ├── screen.tsx.template
│       └── imports.ts.template
├── generators/
│   ├── KotlinGenerator.kt
│   ├── SwiftGenerator.kt
│   └── TypeScriptGenerator.kt
└── core/
    ├── ComponentMapper.kt
    ├── TypeConverter.kt
    └── CodeFormatter.kt
```

### 2. Generation Pipeline

```
DSL AST → Component Model → Target AST → Code Generation → Formatting
```

1. **Parse DSL**: Convert DSL to AST (Abstract Syntax Tree)
2. **Build Model**: Create `ComponentModel` instances
3. **Map Components**: Map to target-specific components
4. **Generate Code**: Use templates to generate code
5. **Format**: Apply language-specific formatting

### 3. Component Mapping Strategy

Create a `ComponentMapper` that handles target-specific translations:

```kotlin
interface ComponentMapper {
    fun mapComponent(component: ComponentModel): TargetComponent
    fun mapProperty(name: String, value: String, type: PropertyType): TargetProperty
    fun mapCallback(name: String, params: List<CallbackParameter>): TargetCallback
}

class KotlinComposeMapper : ComponentMapper {
    override fun mapComponent(component: ComponentModel): TargetComponent {
        return when (component.type) {
            "Button" -> KotlinButton(component)
            "Text" -> KotlinText(component)
            "Container" -> KotlinContainer(component)
            "ColorPicker" -> KotlinColorPicker(component)
            else -> error("Unknown component: ${component.type}")
        }
    }
}
```

### 4. Template System

Use a templating engine like StringTemplate, Mustache, or KotlinPoet:

```kotlin
// Example with KotlinPoet
fun generateKotlinComponent(component: ComponentModel): FileSpec {
    val fileSpec = FileSpec.builder(packageName, fileName)

    // Add imports
    fileSpec.addImport("androidx.compose.runtime", "*")
    fileSpec.addImport("androidx.compose.material3", "*")

    // Add composable function
    val composableFunc = FunSpec.builder(component.type)
        .addAnnotation(Composable::class)
        .addModifiers(KModifier.PUBLIC)
        .addParameter("modifier", Modifier::class)

    // Add properties as parameters
    component.properties.forEach { (name, value) ->
        composableFunc.addParameter(name, mapPropertyType(value))
    }

    // Add function body
    composableFunc.addCode(generateBody(component))

    fileSpec.addFunction(composableFunc.build())

    return fileSpec.build()
}
```

### 5. Type Safety

Maintain type safety through validation:

```kotlin
class TypeValidator {
    fun validate(component: ComponentModel, descriptor: ComponentDescriptor) {
        // Check required properties
        descriptor.properties.values
            .filter { it.required }
            .forEach { prop ->
                require(component.properties.containsKey(prop.name)) {
                    "Missing required property: ${prop.name}"
                }
            }

        // Check property types
        component.properties.forEach { (name, value) ->
            val propDescriptor = descriptor.properties[name]
            requireNotNull(propDescriptor) { "Unknown property: $name" }

            validatePropertyType(value, propDescriptor.type)
        }

        // Check callbacks
        // ... similar validation
    }
}
```

### 6. Error Handling

Provide clear error messages:

```kotlin
sealed class CodeGenError {
    data class InvalidComponent(
        val componentType: String,
        val message: String
    ) : CodeGenError()

    data class InvalidProperty(
        val componentType: String,
        val propertyName: String,
        val expectedType: PropertyType,
        val actualValue: String
    ) : CodeGenError()

    data class MissingRequiredProperty(
        val componentType: String,
        val propertyName: String
    ) : CodeGenError()
}

sealed class CodeGenResult<out T> {
    data class Success<T>(val value: T) : CodeGenResult<T>()
    data class Failure(val errors: List<CodeGenError>) : CodeGenResult<Nothing>()
}
```

### 7. Testing Strategy

Generate test files alongside components:

```kotlin
// Test generation
fun generateKotlinTest(component: ComponentModel): FileSpec {
    return FileSpec.builder(packageName, "${component.type}Test")
        .addImport("org.junit.Test", "")
        .addImport("androidx.compose.ui.test", "*")
        .addType(
            TypeSpec.classBuilder("${component.type}Test")
                .addFunction(generatePreviewTest(component))
                .addFunction(generateInteractionTest(component))
                .build()
        )
        .build()
}
```

### 8. Performance Optimization

- **Lazy Generation**: Generate code only when needed
- **Caching**: Cache generated code and templates
- **Parallel Generation**: Generate multiple files in parallel
- **Incremental Updates**: Only regenerate changed components

### 9. Code Formatting

Apply platform-specific formatting:

```kotlin
class CodeFormatter {
    fun formatKotlin(code: String): String {
        // Use ktlint or similar
        return KtLint.format(code)
    }

    fun formatSwift(code: String): String {
        // Use SwiftFormat or similar
        return SwiftFormat.format(code)
    }

    fun formatTypeScript(code: String): String {
        // Use Prettier or similar
        return Prettier.format(code)
    }
}
```

### 10. Documentation Generation

Generate documentation alongside code:

```kotlin
fun generateDocumentation(component: ComponentModel): String {
    return """
    # ${component.type}

    ## Properties
    ${component.properties.map { (name, value) -> "- `$name`: $value" }.joinToString("\n")}

    ## Callbacks
    ${component.callbacks.map { "- `${it.name}`" }.joinToString("\n")}

    ## Example
    ```kotlin
    ${generateExample(component)}
    ```
    """.trimIndent()
}
```

### 11. Recommended Tools

**Kotlin Generation**:
- [KotlinPoet](https://square.github.io/kotlinpoet/) - Type-safe Kotlin code generation
- [ktlint](https://pinterest.github.io/ktlint/) - Kotlin linter and formatter

**Swift Generation**:
- [SwiftSyntax](https://github.com/apple/swift-syntax) - Swift AST manipulation
- [SwiftFormat](https://github.com/nicklockwood/SwiftFormat) - Swift code formatter

**TypeScript Generation**:
- [ts-morph](https://ts-morph.com/) - TypeScript AST manipulation
- [Prettier](https://prettier.io/) - Code formatter

**Template Engines**:
- [Mustache](https://mustache.github.io/) - Logic-less templates
- [Handlebars](https://handlebarsjs.com/) - Extended Mustache
- [Jinja2](https://jinja.palletsprojects.com/) - Python template engine (if using Python tooling)

### 12. Build Integration

Integrate with build systems:

```kotlin
// Gradle plugin
class AvaCodePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.tasks.register("generateAvaCode", AvaCodeTask::class.java) {
            group = "avacode"
            description = "Generate code from AvaCode DSL"

            inputFiles.set(
                target.fileTree("src/main/avacode") {
                    include("**/*.magic")
                }
            )

            outputDir.set(
                target.layout.buildDirectory.dir("generated/avacode")
            )
        }
    }
}
```

---

## Summary

This document provides comprehensive mappings for code generation in AvaCode:

1. **5 Built-in Components** fully documented with all properties and callbacks
2. **Complete Mappings** for Kotlin Compose, SwiftUI, and React/TypeScript
3. **State Management** patterns for each platform
4. **Callback Patterns** with type-safe signatures
5. **Import Templates** for each target
6. **Code Structure** templates following best practices
7. **Full Working Examples** demonstrating complete applications
8. **Type Conversions** for cross-platform compatibility
9. **Code Generation Strategy** with recommended architecture

### Next Steps

1. **Implement Core Generator**: Build the base `CodeGenerator` interface
2. **Create Target Generators**: Implement `KotlinGenerator`, `SwiftGenerator`, `TypeScriptGenerator`
3. **Build Template System**: Create templates for each target
4. **Add Type Validation**: Implement robust type checking
5. **Generate Tests**: Create test generation alongside components
6. **Build CLI Tool**: Create command-line interface for code generation
7. **IDE Integration**: Create IDE plugins for live generation

This design provides a solid foundation for building a robust, type-safe, multi-target code generator for AvaCode.
