# MagicUI Language Server - User Manual

**Version:** 1.0.0
**Date:** 2025-12-24
**Author:** Manoj Jhawar
**License:** Proprietary - Augmentalis ES

---

## Table of Contents

1. [Introduction](#introduction)
2. [Features Overview](#features-overview)
3. [Installation](#installation)
4. [Getting Started](#getting-started)
5. [IDE Integration](#ide-integration)
6. [Working with MagicUI Files](#working-with-magicui-files)
7. [Autocompletion Guide](#autocompletion-guide)
8. [Error Detection](#error-detection)
9. [Navigation Features](#navigation-features)
10. [Theme Generation](#theme-generation)
11. [Configuration](#configuration)
12. [Keyboard Shortcuts](#keyboard-shortcuts)
13. [Tips and Best Practices](#tips-and-best-practices)
14. [Troubleshooting](#troubleshooting)
15. [FAQ](#faq)

---

## 1. Introduction

### What is MagicUI Language Server?

MagicUI Language Server is an intelligent code assistant for MagicUI DSL files. It provides real-time error checking, smart autocompletion, instant documentation, and powerful navigation features directly in your IDE.

**Think of it as:** IntelliSense for Microsoft Word, but for MagicUI development.

### What You Get

✅ **Smart Autocompletion** - Type faster with context-aware suggestions
✅ **Real-Time Error Detection** - Catch mistakes as you type
✅ **Instant Documentation** - Hover over any component for help
✅ **Quick Navigation** - Jump to component definitions instantly
✅ **Theme Generation** - Export themes in 5 formats (Kotlin, YAML, JSON, CSS, XML)
✅ **Code Formatting** - Keep your code clean and consistent

### Supported File Types

| Extension | Description | Example |
|-----------|-------------|---------|
| `.magic.yaml` | YAML-based MagicUI DSL | `login-screen.magic.yaml` |
| `.magic.json` | JSON-based MagicUI DSL | `dashboard.magic.json` |
| `.magicui` | Compact MagicUI syntax | `settings.magicui` |
| `.ucd` | UI Component Definition | `profile.ucd` |

---

## 2. Features Overview

### 2.1 Intelligent Autocompletion

**What it does:**
- Suggests components as you type (Button, TextField, Card, etc.)
- Offers properties based on component type
- Provides value suggestions (colors, alignments, etc.)

**Example:**

```yaml
# Type "Bu" → suggests "Button"
Button:
  # Type "vu" → suggests "vuid"
  vuid: login-submit
  # Type "te" → suggests "text"
  text: Log In
  # Type "color: " → suggests "red", "blue", "#FF0000", etc.
  color: blue
```

### 2.2 Real-Time Error Checking

**What it does:**
- Detects syntax errors (missing colons, incorrect indentation)
- Validates component rules (Button must have text or icon)
- Checks property values (color format, size units)
- Warns about best practices (ScrollView nesting)

**Visual Indicators:**
- 🔴 Red underline = Error (must fix)
- 🟡 Yellow underline = Warning (should fix)
- ℹ️ Blue underline = Information (consider)

### 2.3 Hover Documentation

**What it does:**
- Shows component descriptions when you hover
- Displays property explanations
- Provides usage examples
- Explains VUID format

**Example:**

Hover over `Button` to see:
```
### Button Component
Interactive button for user actions

Properties:
- text: Button label text
- onClick: Click event handler
- vuid: Voice unique identifier

Example:
Button:
  vuid: submit-btn
  text: Submit
  onClick: handleSubmit
```

### 2.4 Go-to-Definition

**What it does:**
- Jump to VUID declarations
- Navigate component hierarchy
- Find component references

**How to use:**
- Click VUID while holding `Ctrl` (Windows/Linux) or `Cmd` (Mac)
- Or: Right-click → "Go to Definition"
- Or: Press `F12`

### 2.5 Theme Generation

**What it does:**
- Convert theme definitions to multiple formats
- Export for different platforms (Android, iOS, Web)
- Validate theme structure

**Supported Formats:**
1. **Kotlin DSL** - For Android/KMP apps
2. **YAML** - For configuration files
3. **JSON** - For web apps
4. **CSS** - For web styling
5. **Android XML** - For Android resources

---

## 3. Installation

### 3.1 Prerequisites

**Required:**
- ✅ Java 17 or higher
- ✅ VS Code 1.75+ or IntelliJ IDEA 2023.1+

**Check Java version:**

```bash
java -version
# Should show: java version "17" or higher
```

**Don't have Java 17?** Download from:
- [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
- [OpenJDK](https://adoptium.net/)

### 3.2 Download Language Server

**Option 1: Pre-built JAR (Recommended)**

```bash
# Download from releases page
curl -O https://releases.augmentalis.es/LanguageServer-1.0.0.jar

# Verify download
ls -lh LanguageServer-1.0.0.jar
```

**Option 2: Build from Source**

```bash
# Clone repository
cd Modules/AVAMagic/MagicTools/LanguageServer

# Build JAR
./scripts/package.sh

# Output: build/libs/LanguageServer-1.0.0.jar
```

### 3.3 Install VS Code Extension

**Method 1: From VSIX file (when available)**

```bash
code --install-extension magicui-lsp-1.0.0.vsix
```

**Method 2: Build from source**

```bash
cd Modules/AVAMagic/MagicTools/LanguageServer/vscode
npm install
npm run compile
code --install-extension .
```

### 3.4 Verify Installation

**VS Code:**
1. Open VS Code
2. Go to Extensions (Ctrl+Shift+X)
3. Search for "MagicUI"
4. Should see "MagicUI Language Support" installed

**Test:**
1. Create file `test.magic.yaml`
2. Type "Button:"
3. Should see error: "Button should have 'text' or 'icon' property"
4. ✅ Language Server is working!

---

## 4. Getting Started

### 4.1 Your First MagicUI File

**Step 1: Create File**

```bash
# Create new file
touch login-screen.magic.yaml
```

**Step 2: Open in VS Code**

```bash
code login-screen.magic.yaml
```

**Step 3: Start Typing**

Type `Button:` and press Enter. The Language Server will:
- ✅ Highlight the component
- ⚠️ Show warning: "Button should have 'text' or 'icon' property"

**Step 4: Add Properties**

```yaml
Button:
  vuid:    # ← Type here, get suggestions
```

When you type `vuid: `, you'll get:
- Auto-suggestion for VUID format
- Hover documentation explaining VUID

**Step 5: Complete the Component**

```yaml
Button:
  vuid: login-submit-btn
  text: Log In
  onClick: handleLogin
  color: blue
```

**Step 6: See It Work**

- ✅ No errors or warnings
- Hover over `Button` → See documentation
- Hover over `vuid` → See VUID format rules
- Ctrl+Click on `login-submit-btn` → Navigate to definition

---

## 5. IDE Integration

### 5.1 VS Code Setup

**Step 1: Configure JAR Path**

Open VS Code Settings (`Ctrl+,` or `Cmd+,`):

```json
{
    "magicui.server.jarPath": "/absolute/path/to/LanguageServer-1.0.0.jar"
}
```

**💡 Tip:** Use absolute path, not relative!

**Example (Windows):**
```json
"magicui.server.jarPath": "C:\\Users\\YourName\\Downloads\\LanguageServer-1.0.0.jar"
```

**Example (Mac/Linux):**
```json
"magicui.server.jarPath": "/Users/yourname/Downloads/LanguageServer-1.0.0.jar"
```

**Step 2: Enable Trace (Optional, for debugging)**

```json
{
    "magicui.trace.server": "verbose"
}
```

**Values:**
- `off` - No logging (default)
- `messages` - Log messages only
- `verbose` - Log everything

**Step 3: Restart VS Code**

Command Palette (`Ctrl+Shift+P`) → "Reload Window"

### 5.2 IntelliJ IDEA / Android Studio Setup

**Step 1: Install LSP Support Plugin**

1. Go to: `File → Settings → Plugins`
2. Search: "LSP Support"
3. Install: "LSP Support" by JetBrains

**Step 2: Configure Language Server**

1. Go to: `Settings → Languages & Frameworks → Language Server Protocol → Server Definitions`
2. Click `+` to add new server
3. Fill in:
   - **Extension**: `magicui`
   - **Command**: `java -jar /path/to/LanguageServer-1.0.0.jar`

**Step 3: Associate File Patterns**

Add file patterns:
- `*.magic.yaml`
- `*.magic.json`
- `*.magicui`
- `*.ucd`

**Step 4: Apply and Restart**

Click "Apply" → Restart IDE

### 5.3 Other IDEs (Vim, Neovim, Emacs)

**LSP Configuration:**

All editors supporting LSP can use MagicUI Language Server.

**Example (coc.nvim for Neovim):**

```json
{
    "languageserver": {
        "magicui": {
            "command": "java",
            "args": ["-jar", "/path/to/LanguageServer-1.0.0.jar"],
            "filetypes": ["yaml", "json"],
            "rootPatterns": [".git/"]
        }
    }
}
```

---

## 6. Working with MagicUI Files

### 6.1 Supported Components (10 Total)

| Component | Purpose | Required Properties |
|-----------|---------|---------------------|
| **Button** | Clickable button | `text` OR `icon` |
| **TextField** | Text input field | `vuid` (recommended) |
| **Card** | Container with elevation | `children` |
| **Text** | Display text | `text` |
| **Image** | Display image | `src` OR `icon` |
| **Column** | Vertical layout | `children` |
| **Row** | Horizontal layout | `children` |
| **Container** | Generic container | `children` |
| **Checkbox** | Boolean input | `checked` |
| **Switch** | Toggle switch | `enabled` |

### 6.2 Common Properties (All Components)

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `vuid` | String | Voice Unique ID | `login-email-field` |
| `visible` | Boolean | Visibility state | `true`, `false` |
| `enabled` | Boolean | Enabled state | `true`, `false` |
| `style` | Object | Custom styling | `{ backgroundColor: "red" }` |
| `modifiers` | Array | Layout modifiers | `["padding", "margin"]` |

### 6.3 Event Handlers (5 Total)

| Handler | Trigger | Usage |
|---------|---------|-------|
| `onClick` | User clicks | Buttons, clickable items |
| `onChange` | Value changes | TextField, Checkbox, Switch |
| `onSubmit` | Form submission | Forms |
| `onFocus` | Component gains focus | Input fields |
| `onBlur` | Component loses focus | Input fields |

### 6.4 Color Values (9 Named Colors)

| Color | Hex Value | Usage |
|-------|-----------|-------|
| `red` | `#FF0000` | Errors, warnings |
| `blue` | `#0000FF` | Primary actions |
| `green` | `#00FF00` | Success states |
| `black` | `#000000` | Text |
| `white` | `#FFFFFF` | Backgrounds |
| `gray` | `#808080` | Secondary text |
| `yellow` | `#FFFF00` | Highlights |
| `orange` | `#FFA500` | Accents |
| `purple` | `#800080` | Special states |

**Custom Colors:**

```yaml
# Hex (RGB)
color: "#F00"

# Hex (RRGGBB)
color: "#FF0000"

# Hex with alpha (AARRGGBB)
color: "#80FF0000"  # 50% transparent red

# Named
color: blue
```

### 6.5 Size Units

| Unit | Full Name | Use Case | Example |
|------|-----------|----------|---------|
| `dp` | Density-independent pixels | Recommended for layouts | `16dp` |
| `sp` | Scale-independent pixels | For text sizes | `14sp` |
| `px` | Physical pixels | Rare, specific needs | `100px` |
| `%` | Percentage | Relative sizing | `50%` |

**Examples:**

```yaml
Button:
  width: 200dp      # 200 density-independent pixels
  height: 48dp      # Standard button height
  padding: 16dp     # 16dp padding
  fontSize: 14sp    # Text size (scales with user preferences)
  margin: 8dp
```

### 6.6 Alignment Values (7 Total)

| Alignment | Description | Use Case |
|-----------|-------------|----------|
| `start` | Left (LTR) / Right (RTL) | Respects text direction |
| `center` | Center horizontally/vertically | Centered content |
| `end` | Right (LTR) / Left (RTL) | Respects text direction |
| `top` | Top edge | Vertical alignment |
| `bottom` | Bottom edge | Vertical alignment |
| `left` | Left edge | Explicit left |
| `right` | Right edge | Explicit right |

---

## 7. Autocompletion Guide

### 7.1 Component Completion

**Trigger:** Type component name or first letters

```yaml
# Type "Bu" → Get suggestions:
# - Button
# - (no other matches starting with "Bu")

# Press Enter → Auto-expands to:
Button:
  vuid: button-id     # ← Cursor here (editable placeholder)
  text: Click me
  onClick: handleClick
```

**All 10 Components have snippets!**

### 7.2 Property Completion

**Trigger:** Type property name inside component

```yaml
Button:
  # Type "vu" → Suggests "vuid"
  # Type "te" → Suggests "text"
  # Type "on" → Suggests "onClick", "onChange", "onSubmit", etc.
```

**Context-Aware:**

The Language Server knows what component you're in and suggests relevant properties.

```yaml
Button:
  # Suggests: onClick, onChange, text, icon, enabled, visible, vuid

TextField:
  # Suggests: placeholder, value, onChange, validation, vuid

Image:
  # Suggests: src, alt, width, height, vuid
```

### 7.3 Value Completion

**Trigger:** Type property value after colon

**Color Properties:**

```yaml
Button:
  color:   # ← Type here
  # Suggestions: red, blue, green, black, white, gray, yellow, orange, purple
```

**Alignment Properties:**

```yaml
Column:
  alignment:   # ← Type here
  # Suggestions: start, center, end, top, bottom, left, right
```

**Boolean Properties:**

```yaml
Button:
  visible:   # ← Type here
  # Suggestions: true, false
```

### 7.4 Trigger Characters

Autocompletion triggers automatically when you type:

| Character | Triggers |
|-----------|----------|
| `.` | Property access |
| `:` | Property value |
| `-` | VUID suggestions |
| `Space` | General suggestions |

**Manual Trigger:**

Press `Ctrl+Space` (Windows/Linux) or `Cmd+Space` (Mac) to manually trigger completion.

---

## 8. Error Detection

### 8.1 Error Types

**🔴 Errors (Must Fix)**

These prevent your UI from rendering correctly:

```yaml
Image:
  # ❌ Error: Image must have 'src' or 'icon' property
  vuid: profile-pic
  # Missing src!
```

**Fix:**

```yaml
Image:
  vuid: profile-pic
  src: "profile.png"  # ✅ Fixed
```

**🟡 Warnings (Should Fix)**

These indicate potential issues:

```yaml
Button:
  # ⚠️ Warning: Button should have 'text' or 'icon' property
  vuid: submit-btn
  onClick: handleSubmit
  # Missing text/icon - button will be invisible!
```

**Fix:**

```yaml
Button:
  vuid: submit-btn
  text: Submit  # ✅ Fixed
  onClick: handleSubmit
```

**ℹ️ Information (Consider)**

These are best-practice suggestions:

```yaml
Button:
  # ℹ️ Info: Consider adding 'vuid' property for voice navigation
  text: Click Me
  onClick: handleClick
```

### 8.2 Common Validation Errors

**Invalid Color:**

```yaml
Text:
  color: notacolor  # ❌ Invalid color value
```

**Fix:**

```yaml
Text:
  color: red        # ✅ Named color
  # OR
  color: "#FF0000"  # ✅ Hex color
```

**Invalid Size:**

```yaml
Button:
  width: 100  # ❌ Invalid size value (missing unit)
```

**Fix:**

```yaml
Button:
  width: 100dp  # ✅ With unit
```

**Invalid Alignment:**

```yaml
Column:
  alignment: middle  # ❌ Invalid alignment value
```

**Fix:**

```yaml
Column:
  alignment: center  # ✅ Valid alignment
```

**Nested ScrollViews:**

```yaml
ScrollView:
  children:
    - ScrollView:  # ⚠️ Warning: ScrollView should not contain another ScrollView
        children: []
```

**Fix:**

```yaml
ScrollView:
  children:
    - Column:  # ✅ Use Column or Container instead
        children: []
```

### 8.3 How to Fix Errors

**Method 1: Hover for Details**

1. Hover over underlined text
2. Read error message
3. Follow suggested fix

**Method 2: Check Problems Panel**

**VS Code:**
- View → Problems (`Ctrl+Shift+M`)
- Lists all errors/warnings in project
- Click error to jump to line

**IntelliJ:**
- View → Tool Windows → Problems
- Lists all issues

**Method 3: Use Autocomplete**

If unsure about valid values, trigger autocomplete (`Ctrl+Space`) to see suggestions.

---

## 9. Navigation Features

### 9.1 Go-to-Definition (VUID Navigation)

**What it does:**

Jump from VUID reference to VUID declaration.

**Example:**

```yaml
# Declaration
Button:
  vuid: submit-button  # ← Definition
  text: Submit

# Reference (somewhere else in file)
Container:
  onClick: submit-button  # ← Reference (Ctrl+Click to jump to definition)
```

**How to use:**

**Method 1: Ctrl+Click**
- Hold `Ctrl` (Windows/Linux) or `Cmd` (Mac)
- Click on VUID reference
- Jumps to declaration

**Method 2: Keyboard**
- Place cursor on VUID reference
- Press `F12`
- Jumps to declaration

**Method 3: Context Menu**
- Right-click on VUID reference
- Select "Go to Definition"

### 9.2 Find All References

**What it does:**

Find all places where a VUID is used.

**How to use:**

1. Place cursor on VUID declaration
2. Press `Shift+F12` (VS Code) or `Alt+F7` (IntelliJ)
3. See all references in sidebar

### 9.3 Peek Definition

**What it does:**

View definition inline without jumping away.

**How to use:**

1. Place cursor on VUID reference
2. Press `Alt+F12` (VS Code) or `Ctrl+Shift+I` (IntelliJ)
3. See definition in popup window

---

## 10. Theme Generation

### 10.1 What is Theme Generation?

Convert theme definitions from MagicUI format to platform-specific formats:

- **Kotlin DSL** → Android/KMP apps
- **YAML** → Configuration files
- **JSON** → Web apps, REST APIs
- **CSS** → Web styling
- **Android XML** → Android resources

### 10.2 Creating a Theme

**Step 1: Define Theme**

Create `my-theme.magic.yaml`:

```yaml
Theme:
  name: DarkTheme
  colors:
    primary: "#BB86FC"
    secondary: "#03DAC6"
    background: "#121212"
    surface: "#1E1E1E"
    error: "#CF6679"
    onPrimary: "#000000"
    onSecondary: "#000000"
    onBackground: "#FFFFFF"
    onSurface: "#FFFFFF"
  typography:
    h1:
      fontSize: 32sp
      fontWeight: bold
    body:
      fontSize: 14sp
      fontWeight: normal
  spacing:
    small: 8dp
    medium: 16dp
    large: 24dp
```

### 10.3 Exporting Theme

**Method 1: VS Code Command Palette**

1. Open theme file
2. Press `Ctrl+Shift+P` (Windows/Linux) or `Cmd+Shift+P` (Mac)
3. Type: "MagicUI: Generate Theme"
4. Select format: Kotlin DSL, YAML, JSON, CSS, or Android XML
5. Output appears in new file

**Method 2: Programmatic (TypeScript)**

```typescript
import * as vscode from 'vscode';

async function generateTheme() {
    const themeJson = JSON.stringify({
        name: "DarkTheme",
        colors: {
            primary: "#BB86FC",
            secondary: "#03DAC6"
        }
    });

    const result = await vscode.workspace.executeCommand(
        'magicui.generateTheme',
        'dsl',  // Format: dsl, yaml, json, css, xml
        themeJson
    );

    console.log(result.output);
}
```

### 10.4 Theme Output Examples

**Kotlin DSL Output:**

```kotlin
object DarkTheme : Theme {
    override val colors = ColorScheme(
        primary = Color(0xBB86FC),
        secondary = Color(0x03DAC6),
        background = Color(0x121212),
        // ...
    )

    override val typography = Typography(
        h1 = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold),
        body = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
    )
}
```

**CSS Output:**

```css
:root[data-theme="dark"] {
    --primary: #BB86FC;
    --secondary: #03DAC6;
    --background: #121212;
    --surface: #1E1E1E;
    --error: #CF6679;

    --font-h1: bold 32px sans-serif;
    --font-body: normal 14px sans-serif;

    --spacing-small: 8px;
    --spacing-medium: 16px;
    --spacing-large: 24px;
}
```

**Android XML Output:**

```xml
<resources>
    <color name="primary">#BB86FC</color>
    <color name="secondary">#03DAC6</color>
    <color name="background">#121212</color>
    <color name="surface">#1E1E1E</color>
    <color name="error">#CF6679</color>

    <dimen name="spacing_small">8dp</dimen>
    <dimen name="spacing_medium">16dp</dimen>
    <dimen name="spacing_large">24dp</dimen>
</resources>
```

---

## 11. Configuration

### 11.1 VS Code Settings

**Location:**

- **User Settings:** `File → Preferences → Settings` (applies to all projects)
- **Workspace Settings:** `.vscode/settings.json` (applies to current project only)

**Available Settings:**

```json
{
    // Required: Path to Language Server JAR
    "magicui.server.jarPath": "/path/to/LanguageServer-1.0.0.jar",

    // Optional: Logging level (off, messages, verbose)
    "magicui.trace.server": "off",

    // Optional: Auto-format on save
    "editor.formatOnSave": true,

    // Optional: Trigger completion on specific characters
    "editor.quickSuggestions": {
        "other": true,
        "comments": false,
        "strings": true
    }
}
```

### 11.2 File Associations

**Auto-detect MagicUI files:**

```json
{
    "files.associations": {
        "*.magic.yaml": "yaml",
        "*.magic.json": "json",
        "*.magicui": "yaml",
        "*.ucd": "yaml"
    }
}
```

### 11.3 Logging Configuration

**Server-side logging:**

Edit `logback.xml` in Language Server directory:

```xml
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.FileAppender">
        <file>magicui-lsp.log</file>
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Change level to DEBUG for verbose logging -->
    <logger name="com.augmentalis.magicui.lsp" level="INFO" />

    <root level="INFO">
        <appender-ref ref="FILE" />
    </root>
</configuration>
```

**Log Levels:**
- `ERROR` - Errors only
- `WARN` - Warnings and errors
- `INFO` - General information (default)
- `DEBUG` - Detailed information
- `TRACE` - Very detailed (all LSP messages)

---

## 12. Keyboard Shortcuts

### 12.1 VS Code Shortcuts

| Action | Windows/Linux | Mac | Description |
|--------|---------------|-----|-------------|
| **Completion** | `Ctrl+Space` | `Cmd+Space` | Trigger autocomplete |
| **Hover** | Hover mouse | Hover mouse | Show documentation |
| **Go to Definition** | `F12` | `F12` | Jump to VUID definition |
| **Peek Definition** | `Alt+F12` | `Opt+F12` | View definition inline |
| **Find References** | `Shift+F12` | `Shift+F12` | Find all VUID uses |
| **Format Document** | `Shift+Alt+F` | `Shift+Opt+F` | Format current file |
| **Problems Panel** | `Ctrl+Shift+M` | `Cmd+Shift+M` | Show errors/warnings |
| **Command Palette** | `Ctrl+Shift+P` | `Cmd+Shift+P` | Run commands |

### 12.2 IntelliJ / Android Studio Shortcuts

| Action | Windows/Linux | Mac | Description |
|--------|---------------|-----|-------------|
| **Completion** | `Ctrl+Space` | `Ctrl+Space` | Trigger autocomplete |
| **Quick Documentation** | `Ctrl+Q` | `F1` | Show documentation |
| **Go to Definition** | `Ctrl+B` | `Cmd+B` | Jump to VUID definition |
| **Quick Definition** | `Ctrl+Shift+I` | `Opt+Space` | View definition inline |
| **Find Usages** | `Alt+F7` | `Opt+F7` | Find all VUID uses |
| **Reformat Code** | `Ctrl+Alt+L` | `Cmd+Opt+L` | Format current file |
| **Show Intentions** | `Alt+Enter` | `Opt+Enter` | Quick fixes |

### 12.3 Custom Keybindings

**VS Code:**

Edit `keybindings.json`:

```json
[
    {
        "key": "ctrl+shift+g",
        "command": "workbench.action.tasks.runTask",
        "args": "MagicUI: Generate Theme"
    }
]
```

**IntelliJ:**

Settings → Keymap → Search for "LSP" → Assign custom shortcut

---

## 13. Tips and Best Practices

### 13.1 VUID Naming Conventions

**Pattern:** `component-type-descriptor`

**Good Examples:**
- ✅ `login-email-field` (clear, descriptive)
- ✅ `dashboard-user-avatar` (includes context)
- ✅ `settings-dark-mode-switch` (specific)

**Bad Examples:**
- ❌ `btn1` (too generic)
- ❌ `textfield` (no context)
- ❌ `MyButton` (uppercase not allowed)

**Best Practices:**
1. **Be descriptive** - `submit-button` not `btn`
2. **Include context** - `profile-edit-button` not `edit-button`
3. **Use hyphens** - `email-field` not `email_field` or `emailField`
4. **Lowercase only** - `login-btn` not `LoginBtn`
5. **Keep reasonable length** - 3-40 characters ideal

### 13.2 Component Hierarchy

**Do:**
- ✅ Use layout components (Column, Row, Container) to organize UI
- ✅ Keep nesting depth under 10 levels
- ✅ Use descriptive VUIDs at each level

**Don't:**
- ❌ Nest ScrollViews (causes scroll conflicts)
- ❌ Create overly deep hierarchies (hard to maintain)
- ❌ Mix layout patterns (stick to Column/Row OR Container)

**Example: Good Hierarchy**

```yaml
Screen:
  vuid: login-screen
  children:
    - Column:
        vuid: login-form
        children:
          - TextField:
              vuid: login-email
          - TextField:
              vuid: login-password
          - Button:
              vuid: login-submit
```

### 13.3 Performance Tips

**Autocompletion:**
- Type first few letters before waiting for suggestions
- Use Tab to accept suggestions faster than Enter
- Dismiss suggestions with Esc if not needed

**Error Checking:**
- Fix errors as they appear (don't accumulate)
- Use Problems panel to see all issues at once
- Configure auto-save to see validation in real-time

**Theme Generation:**
- Keep theme files under 500 lines
- Split large themes into sections
- Use YAML for readability, JSON for performance

### 13.4 Organization

**File Structure:**

```
project/
├── screens/
│   ├── login-screen.magic.yaml
│   ├── dashboard-screen.magic.yaml
│   └── settings-screen.magic.yaml
├── components/
│   ├── buttons.magic.yaml
│   ├── cards.magic.yaml
│   └── forms.magic.yaml
└── themes/
    ├── light-theme.magic.yaml
    └── dark-theme.magic.yaml
```

**Benefits:**
- ✅ Easy to find files
- ✅ Better git diffs
- ✅ Simpler collaboration

---

## 14. Troubleshooting

### 14.1 Language Server Not Starting

**Symptom:** No autocompletion, no error checking

**Diagnosis:**

1. **Check Extension Installed:**
   - VS Code: Extensions panel → Search "MagicUI"
   - Should see "MagicUI Language Support"

2. **Check JAR Path:**
   - Open VS Code Settings
   - Search: "magicui.server.jarPath"
   - Verify path is correct and absolute

3. **Check Java Version:**
   ```bash
   java -version
   # Must be 17 or higher
   ```

4. **Check Server Status:**
   - VS Code: View → Output → Select "MagicUI Language Server"
   - Look for "Language Server listening on stdio"

**Fix:**

```json
// VS Code settings.json
{
    "magicui.server.jarPath": "/absolute/path/to/LanguageServer-1.0.0.jar"
}
```

Then: Command Palette → "Reload Window"

### 14.2 No Autocompletion Appearing

**Symptom:** Typing doesn't show suggestions

**Diagnosis:**

1. **Check File Extension:**
   - Must be `.magic.yaml`, `.magic.json`, `.magicui`, or `.ucd`

2. **Check File Association:**
   ```json
   {
       "files.associations": {
           "*.magic.yaml": "yaml"
       }
   }
   ```

3. **Trigger Manually:**
   - Press `Ctrl+Space` (Windows/Linux) or `Cmd+Space` (Mac)
   - If suggestions appear, autocomplete is working

**Fix:**

Rename file with correct extension:
```bash
mv myfile.yaml myfile.magic.yaml
```

### 14.3 Errors Not Showing

**Symptom:** File has issues but no red/yellow underlines

**Diagnosis:**

1. **Check Problems Panel:**
   - View → Problems (`Ctrl+Shift+M`)
   - If errors listed here, diagnostics are working

2. **Check File Content:**
   - File must have valid YAML/JSON structure
   - Completely empty files won't show errors

3. **Check Logs:**
   ```bash
   tail -f magicui-lsp.log
   # Look for "Validated document" messages
   ```

**Fix:**

1. Save file (`Ctrl+S`)
2. Close and reopen file
3. Restart Language Server (Command Palette → "Reload Window")

### 14.4 Performance Issues

**Symptom:** Slow autocompletion, laggy typing

**Diagnosis:**

1. **Check File Size:**
   ```bash
   wc -l myfile.magic.yaml
   # If >5000 lines, consider splitting
   ```

2. **Check CPU Usage:**
   - Task Manager / Activity Monitor
   - Java process should be <10% CPU when idle

3. **Check Logs:**
   ```bash
   tail -f magicui-lsp.log
   # Look for "Validation took XXXms" messages
   ```

**Fix:**

1. **Split Large Files:**
   ```bash
   # Split screens into separate files
   mv dashboard-screen.magic.yaml screens/
   ```

2. **Disable Auto-Save (if enabled):**
   ```json
   {
       "files.autoSave": "off"
   }
   ```

3. **Reduce Logging:**
   ```json
   {
       "magicui.trace.server": "off"
   }
   ```

### 14.5 Theme Generation Fails

**Symptom:** "Theme generation failed" error

**Diagnosis:**

1. **Check Theme Structure:**
   - Must have `name` property
   - Must have `colors` section

2. **Check JSON Format:**
   ```typescript
   // Must be valid JSON
   const themeJson = JSON.stringify({ /* ... */ });
   ```

3. **Check Logs:**
   ```bash
   tail -f magicui-lsp.log | grep "Theme generation"
   ```

**Fix:**

Validate theme structure:
```yaml
Theme:
  name: MyTheme  # ← Required
  colors:        # ← Required
    primary: "#FF0000"
    # ... more colors
```

---

## 15. FAQ

### Q1: What file types does the Language Server support?

**A:** Four file types:
- `.magic.yaml` - YAML-based MagicUI DSL (recommended)
- `.magic.json` - JSON-based MagicUI DSL
- `.magicui` - Compact MagicUI syntax
- `.ucd` - UI Component Definition files

### Q2: Can I use the Language Server without VS Code?

**A:** Yes! The Language Server uses the standard LSP protocol and works with any LSP-compatible editor:
- IntelliJ IDEA / Android Studio (with LSP Support plugin)
- Neovim (with coc.nvim or nvim-lspconfig)
- Vim (with ale or LanguageClient-neovim)
- Emacs (with lsp-mode)
- Sublime Text (with LSP package)

### Q3: How do I update the Language Server?

**A:**
1. Download new JAR file
2. Update `magicui.server.jarPath` in settings
3. Restart editor (Command Palette → "Reload Window")

### Q4: Can I use the Language Server offline?

**A:** Yes! The Language Server runs entirely locally and doesn't require internet connection. Once the JAR file is downloaded, it works offline.

### Q5: Does the Language Server support multi-file projects?

**A:** Yes! The Language Server validates each file independently. You can have multiple `.magic.yaml` files in a project, and each will be validated separately.

### Q6: How do I report bugs or request features?

**A:**
- **Bugs:** Create issue at project repository with:
  - Steps to reproduce
  - Expected behavior
  - Actual behavior
  - Log output (`magicui-lsp.log`)

- **Features:** Create feature request with:
  - Use case description
  - Expected behavior
  - Example usage

### Q7: Is the Language Server free?

**A:** The Language Server is proprietary software by Augmentalis ES. Licensing terms depend on your agreement. Contact: manoj@ideahq.net

### Q8: Can I extend the Language Server with custom components?

**A:** Not directly through configuration. Custom components require modifying the server source code and rebuilding. See Developer Manual for extension guide.

### Q9: What's the difference between VUID and regular IDs?

**A:**
- **VUID (Voice Unique ID):** Used by VoiceOS for voice navigation ("Click submit-button")
- **Format:** Lowercase, hyphens, descriptive (`login-submit-button`)
- **Regular IDs:** Generic identifiers, no specific format

### Q10: Can I convert existing YAML files to MagicUI?

**A:** Yes, but it requires:
1. Rename file to `.magic.yaml`
2. Ensure structure matches MagicUI component format
3. Add `vuid` properties to components
4. Validate and fix any errors shown by Language Server

---

## 16. Getting Help

### 16.1 Resources

**Documentation:**
- Developer Manual: `MagicUI-LSP-Developer-Manual-251224-V1.md`
- Examples: `Modules/AVAMagic/MagicTools/LanguageServer/EXAMPLES.md`
- README: `Modules/AVAMagic/MagicTools/LanguageServer/README.md`

**Support:**
- Email: manoj@ideahq.net
- Issues: Project repository issue tracker
- Documentation: Project wiki (if available)

### 16.2 Before Asking for Help

**Gather this information:**

1. **Version:**
   ```bash
   java -jar LanguageServer-1.0.0.jar --version
   ```

2. **Java Version:**
   ```bash
   java -version
   ```

3. **IDE Version:**
   - VS Code: Help → About
   - IntelliJ: Help → About

4. **Log Output:**
   ```bash
   tail -n 100 magicui-lsp.log
   ```

5. **Steps to Reproduce:**
   - What you did
   - What you expected
   - What actually happened

6. **File Sample:**
   - Minimal `.magic.yaml` file that reproduces issue

---

**End of User Manual**

For technical details and extending the server, see: `MagicUI-LSP-Developer-Manual-251224-V1.md`
For practical code examples, see: `Modules/AVAMagic/MagicTools/LanguageServer/EXAMPLES.md`
