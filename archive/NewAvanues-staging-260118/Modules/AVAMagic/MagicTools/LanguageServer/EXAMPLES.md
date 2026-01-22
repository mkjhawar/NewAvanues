# MagicUI Language Server - Code Examples

**Version:** 1.0.0
**Date:** 2025-12-24
**Purpose:** Practical code examples for common use cases

---

## Table of Contents

1. [Complete UI Screens](#complete-ui-screens)
2. [Component Examples](#component-examples)
3. [Validation Examples](#validation-examples)
4. [Theme Examples](#theme-examples)
5. [Integration Examples](#integration-examples)
6. [Testing Examples](#testing-examples)

---

## 1. Complete UI Screens

### 1.1 Login Screen

**File:** `login-screen.magic.yaml`

```yaml
Screen:
  vuid: login-screen
  title: Login
  children:
    - Column:
        vuid: login-container
        alignment: center
        padding: 24dp
        children:
          - Image:
              vuid: app-logo
              src: "logo.png"
              width: 120dp
              height: 120dp
              alignment: center

          - Text:
              vuid: welcome-text
              text: Welcome Back
              fontSize: 24sp
              color: "#333333"
              alignment: center
              margin: 16dp

          - TextField:
              vuid: login-email-field
              placeholder: Email address
              onChange: handleEmailChange
              width: 100%
              validation:
                type: email
                required: true

          - TextField:
              vuid: login-password-field
              placeholder: Password
              onChange: handlePasswordChange
              width: 100%
              secureTextEntry: true
              validation:
                type: password
                minLength: 8
                required: true

          - Row:
              vuid: remember-row
              alignment: start
              children:
                - Checkbox:
                    vuid: remember-me-checkbox
                    checked: false
                    onChange: handleRememberMe

                - Text:
                    vuid: remember-me-label
                    text: Remember me
                    fontSize: 14sp
                    color: "#666666"

          - Button:
              vuid: login-submit-button
              text: Log In
              onClick: handleLogin
              backgroundColor: blue
              width: 100%
              height: 48dp
              margin: 16dp

          - Text:
              vuid: forgot-password-link
              text: Forgot password?
              fontSize: 14sp
              color: blue
              onClick: handleForgotPassword
              alignment: center
```

**Autocomplete Workflow:**

1. Type `Sc` → Select `Screen`
2. Type `vu` → Select `vuid` → Enter `login-screen`
3. Type `ch` → Select `children`
4. Type `Co` → Select `Column` (auto-expands with snippet)
5. Repeat for child components

**Validation Checks:**
- ✅ All components have `vuid`
- ✅ Button has `text` property
- ✅ TextField has `onChange` handler
- ✅ Image has `src` property
- ✅ No nested ScrollViews

### 1.2 Dashboard Screen

**File:** `dashboard-screen.magic.yaml`

```yaml
Screen:
  vuid: dashboard-screen
  title: Dashboard
  children:
    - ScrollView:
        vuid: dashboard-scroll
        children:
          - Column:
              vuid: dashboard-content
              padding: 16dp
              children:
                # Welcome Section
                - Card:
                    vuid: welcome-card
                    elevation: 2
                    padding: 16dp
                    margin: 8dp
                    backgroundColor: white
                    children:
                      - Row:
                          vuid: welcome-row
                          alignment: start
                          children:
                            - Image:
                                vuid: user-avatar
                                src: "avatar.png"
                                width: 48dp
                                height: 48dp
                                borderRadius: 24dp

                            - Column:
                                vuid: welcome-text-column
                                margin: 8dp
                                children:
                                  - Text:
                                      vuid: greeting-text
                                      text: Good morning, John
                                      fontSize: 18sp
                                      fontWeight: bold

                                  - Text:
                                      vuid: status-text
                                      text: You have 3 new notifications
                                      fontSize: 14sp
                                      color: gray

                # Stats Section
                - Row:
                    vuid: stats-row
                    children:
                      - Card:
                          vuid: total-users-card
                          elevation: 2
                          padding: 16dp
                          margin: 8dp
                          flex: 1
                          children:
                            - Text:
                                vuid: total-users-label
                                text: Total Users
                                fontSize: 12sp
                                color: gray

                            - Text:
                                vuid: total-users-value
                                text: "1,234"
                                fontSize: 24sp
                                fontWeight: bold

                      - Card:
                          vuid: active-sessions-card
                          elevation: 2
                          padding: 16dp
                          margin: 8dp
                          flex: 1
                          children:
                            - Text:
                                vuid: active-sessions-label
                                text: Active Sessions
                                fontSize: 12sp
                                color: gray

                            - Text:
                                vuid: active-sessions-value
                                text: "89"
                                fontSize: 24sp
                                fontWeight: bold
                                color: green

                # Recent Activity
                - Card:
                    vuid: recent-activity-card
                    elevation: 2
                    padding: 16dp
                    margin: 8dp
                    children:
                      - Text:
                          vuid: recent-activity-title
                          text: Recent Activity
                          fontSize: 16sp
                          fontWeight: bold
                          margin: 8dp

                      - Column:
                          vuid: activity-list
                          children:
                            - Row:
                                vuid: activity-item-1
                                padding: 8dp
                                children:
                                  - Text:
                                      vuid: activity-1-text
                                      text: "User John Doe logged in"
                                      fontSize: 14sp

                                  - Text:
                                      vuid: activity-1-time
                                      text: "2 mins ago"
                                      fontSize: 12sp
                                      color: gray

                            - Row:
                                vuid: activity-item-2
                                padding: 8dp
                                children:
                                  - Text:
                                      vuid: activity-2-text
                                      text: "New order #1234 created"
                                      fontSize: 14sp

                                  - Text:
                                      vuid: activity-2-time
                                      text: "5 mins ago"
                                      fontSize: 12sp
                                      color: gray
```

**Hover Documentation:**

Hover over any component to see:
- Component description
- Available properties
- Usage examples
- Related components

### 1.3 Settings Screen

**File:** `settings-screen.magic.yaml`

```yaml
Screen:
  vuid: settings-screen
  title: Settings
  children:
    - ScrollView:
        vuid: settings-scroll
        children:
          - Column:
              vuid: settings-content
              children:
                # Account Section
                - Text:
                    vuid: account-section-title
                    text: Account
                    fontSize: 16sp
                    fontWeight: bold
                    color: "#333333"
                    padding: 16dp

                - Card:
                    vuid: profile-card
                    elevation: 1
                    margin: 8dp
                    children:
                      - Row:
                          vuid: profile-row
                          padding: 16dp
                          onClick: navigateToProfile
                          children:
                            - Text:
                                vuid: profile-label
                                text: Edit Profile
                                fontSize: 14sp

                            - Text:
                                vuid: profile-icon
                                text: "→"
                                alignment: end

                # Preferences Section
                - Text:
                    vuid: preferences-section-title
                    text: Preferences
                    fontSize: 16sp
                    fontWeight: bold
                    color: "#333333"
                    padding: 16dp

                - Card:
                    vuid: preferences-card
                    elevation: 1
                    margin: 8dp
                    children:
                      - Row:
                          vuid: dark-mode-row
                          padding: 16dp
                          children:
                            - Text:
                                vuid: dark-mode-label
                                text: Dark Mode
                                fontSize: 14sp

                            - Switch:
                                vuid: dark-mode-switch
                                enabled: true
                                onChange: handleDarkModeToggle
                                alignment: end

                      - Row:
                          vuid: notifications-row
                          padding: 16dp
                          children:
                            - Text:
                                vuid: notifications-label
                                text: Push Notifications
                                fontSize: 14sp

                            - Switch:
                                vuid: notifications-switch
                                enabled: false
                                onChange: handleNotificationsToggle
                                alignment: end

                      - Row:
                          vuid: sound-row
                          padding: 16dp
                          children:
                            - Text:
                                vuid: sound-label
                                text: Sound Effects
                                fontSize: 14sp

                            - Switch:
                                vuid: sound-switch
                                enabled: true
                                onChange: handleSoundToggle
                                alignment: end

                # About Section
                - Text:
                    vuid: about-section-title
                    text: About
                    fontSize: 16sp
                    fontWeight: bold
                    color: "#333333"
                    padding: 16dp

                - Card:
                    vuid: about-card
                    elevation: 1
                    margin: 8dp
                    children:
                      - Row:
                          vuid: version-row
                          padding: 16dp
                          children:
                            - Text:
                                vuid: version-label
                                text: Version
                                fontSize: 14sp

                            - Text:
                                vuid: version-value
                                text: "1.0.0"
                                fontSize: 14sp
                                color: gray
                                alignment: end

                      - Row:
                          vuid: terms-row
                          padding: 16dp
                          onClick: navigateToTerms
                          children:
                            - Text:
                                vuid: terms-label
                                text: Terms of Service
                                fontSize: 14sp

                            - Text:
                                vuid: terms-icon
                                text: "→"
                                alignment: end

                      - Row:
                          vuid: privacy-row
                          padding: 16dp
                          onClick: navigateToPrivacy
                          children:
                            - Text:
                                vuid: privacy-label
                                text: Privacy Policy
                                fontSize: 14sp

                            - Text:
                                vuid: privacy-icon
                                text: "→"
                                alignment: end

                # Logout Button
                - Button:
                    vuid: logout-button
                    text: Log Out
                    onClick: handleLogout
                    backgroundColor: red
                    color: white
                    margin: 24dp
                    height: 48dp
```

---

## 2. Component Examples

### 2.1 Button Variations

```yaml
# Basic Button
Button:
  vuid: basic-button
  text: Click Me
  onClick: handleClick

# Styled Button
Button:
  vuid: primary-button
  text: Submit
  onClick: handleSubmit
  backgroundColor: blue
  color: white
  width: 200dp
  height: 48dp
  borderRadius: 8dp

# Icon Button
Button:
  vuid: icon-button
  icon: "search"
  onClick: handleSearch
  backgroundColor: transparent

# Disabled Button
Button:
  vuid: disabled-button
  text: Can't Click
  onClick: handleClick
  enabled: false
  backgroundColor: gray
```

**Autocomplete:** Type `Button:` → Get snippet with placeholders

**Validation:**
- ✅ Must have `text` OR `icon`
- ✅ Should have `onClick` handler
- ✅ Should have `vuid` for voice navigation

### 2.2 TextField Variations

```yaml
# Basic TextField
TextField:
  vuid: basic-field
  placeholder: Enter text
  onChange: handleChange

# Email Field
TextField:
  vuid: email-field
  placeholder: Email address
  onChange: handleEmailChange
  validation:
    type: email
    required: true

# Password Field
TextField:
  vuid: password-field
  placeholder: Password
  onChange: handlePasswordChange
  secureTextEntry: true
  validation:
    type: password
    minLength: 8
    required: true

# Multiline TextField
TextField:
  vuid: comment-field
  placeholder: Write a comment...
  onChange: handleCommentChange
  multiline: true
  numberOfLines: 4
  width: 100%
```

**Autocomplete:** Type `TextField:` → Get snippet with common properties

**Hover:** Hover over `validation` to see validation rules documentation

### 2.3 Layout Components

```yaml
# Column (Vertical Layout)
Column:
  vuid: vertical-layout
  alignment: center
  padding: 16dp
  children:
    - Text:
        vuid: item-1
        text: First Item
    - Text:
        vuid: item-2
        text: Second Item
    - Text:
        vuid: item-3
        text: Third Item

# Row (Horizontal Layout)
Row:
  vuid: horizontal-layout
  alignment: start
  padding: 16dp
  children:
    - Text:
        vuid: left-text
        text: Left
    - Text:
        vuid: center-text
        text: Center
        flex: 1
        alignment: center
    - Text:
        vuid: right-text
        text: Right

# Container (Generic Container)
Container:
  vuid: generic-container
  backgroundColor: white
  padding: 16dp
  borderRadius: 8dp
  children:
    - Text:
        vuid: container-text
        text: Content inside container
```

**Validation:**
- ⚠️ Warning if layout component has no children
- ℹ️ Info if nesting depth exceeds 10 levels

### 2.4 Card Component

```yaml
# Basic Card
Card:
  vuid: basic-card
  elevation: 2
  padding: 16dp
  children:
    - Text:
        vuid: card-title
        text: Card Title
        fontSize: 16sp
        fontWeight: bold

    - Text:
        vuid: card-description
        text: This is a card with elevation and padding
        fontSize: 14sp
        color: gray

# Clickable Card
Card:
  vuid: clickable-card
  elevation: 4
  padding: 16dp
  onClick: handleCardClick
  children:
    - Row:
        vuid: card-content
        children:
          - Image:
              vuid: card-icon
              src: "icon.png"
              width: 48dp
              height: 48dp

          - Column:
              vuid: card-text
              margin: 8dp
              children:
                - Text:
                    vuid: card-title
                    text: Title
                    fontSize: 16sp
                    fontWeight: bold

                - Text:
                    vuid: card-subtitle
                    text: Subtitle
                    fontSize: 14sp
                    color: gray
```

---

## 3. Validation Examples

### 3.1 Valid Code Examples

```yaml
# ✅ Valid: Button has text
Button:
  vuid: submit-btn
  text: Submit
  onClick: handleSubmit

# ✅ Valid: Button has icon (alternative to text)
Button:
  vuid: search-btn
  icon: "search"
  onClick: handleSearch

# ✅ Valid: Image has src
Image:
  vuid: profile-pic
  src: "profile.png"
  width: 100dp
  height: 100dp

# ✅ Valid: Color in hex format
Text:
  vuid: colored-text
  text: Hello
  color: "#FF0000"

# ✅ Valid: Size with unit
Button:
  width: 200dp
  height: 48dp

# ✅ Valid: Container has children
Container:
  vuid: wrapper
  children:
    - Text:
        vuid: child
        text: Content
```

### 3.2 Invalid Code Examples (Will Show Errors)

```yaml
# ❌ Error: Button missing text AND icon
Button:
  vuid: broken-btn
  onClick: handleClick
  # Missing: text or icon property

# ❌ Error: Image missing src AND icon
Image:
  vuid: broken-img
  width: 100dp
  height: 100dp
  # Missing: src or icon property

# ❌ Warning: Invalid color value
Text:
  vuid: bad-color-text
  text: Hello
  color: notacolor  # Not a valid color

# ❌ Warning: Size without unit
Button:
  width: 200  # Missing unit (should be 200dp)

# ❌ Warning: Invalid alignment
Column:
  alignment: middle  # Should be "center"

# ⚠️ Warning: Nested ScrollViews
ScrollView:
  children:
    - ScrollView:  # Problematic nesting
        children: []

# ⚠️ Warning: Empty container
Container:
  vuid: empty-container
  # No children - container is empty
```

### 3.3 VUID Format Validation

```yaml
# ✅ Valid VUIDs
Button:
  vuid: login-submit-button  # Lowercase, hyphens, descriptive

TextField:
  vuid: profile-email-field  # Context included

Image:
  vuid: dashboard-user-avatar  # Clear purpose

# ❌ Invalid VUIDs (Will Show Warnings)
Button:
  vuid: LoginButton  # Uppercase not allowed

TextField:
  vuid: btn  # Too short (minimum 3 chars)

Image:
  vuid: button-  # Ends with hyphen (invalid)

Container:
  vuid: my_container  # Underscore not allowed (use hyphens)
```

**Hover over VUIDs** to see format requirements and examples.

---

## 4. Theme Examples

### 4.1 Complete Theme Definition

**File:** `dark-theme.magic.yaml`

```yaml
Theme:
  name: DarkTheme
  description: Dark theme for night mode

  # Color Scheme
  colors:
    # Primary Colors
    primary: "#BB86FC"
    primaryVariant: "#3700B3"
    onPrimary: "#000000"

    # Secondary Colors
    secondary: "#03DAC6"
    secondaryVariant: "#018786"
    onSecondary: "#000000"

    # Background Colors
    background: "#121212"
    onBackground: "#FFFFFF"

    # Surface Colors
    surface: "#1E1E1E"
    onSurface: "#FFFFFF"

    # Error Colors
    error: "#CF6679"
    onError: "#000000"

  # Typography
  typography:
    h1:
      fontSize: 32sp
      fontWeight: bold
      color: "#FFFFFF"
      letterSpacing: 0.5

    h2:
      fontSize: 24sp
      fontWeight: bold
      color: "#FFFFFF"

    h3:
      fontSize: 20sp
      fontWeight: semibold
      color: "#FFFFFF"

    body:
      fontSize: 14sp
      fontWeight: normal
      color: "#CCCCCC"
      lineHeight: 20sp

    caption:
      fontSize: 12sp
      fontWeight: normal
      color: "#999999"

  # Spacing
  spacing:
    xs: 4dp
    small: 8dp
    medium: 16dp
    large: 24dp
    xl: 32dp
    xxl: 48dp

  # Elevation (Shadow Depths)
  elevation:
    level0: 0dp
    level1: 2dp
    level2: 4dp
    level3: 8dp
    level4: 16dp

  # Border Radius
  borderRadius:
    small: 4dp
    medium: 8dp
    large: 16dp
    round: 9999dp
```

### 4.2 Light Theme

**File:** `light-theme.magic.yaml`

```yaml
Theme:
  name: LightTheme
  description: Light theme for day mode

  colors:
    primary: "#6200EE"
    primaryVariant: "#3700B3"
    onPrimary: "#FFFFFF"

    secondary: "#03DAC6"
    secondaryVariant: "#018786"
    onSecondary: "#000000"

    background: "#FFFFFF"
    onBackground: "#000000"

    surface: "#FFFFFF"
    onSurface: "#000000"

    error: "#B00020"
    onError: "#FFFFFF"

  typography:
    h1:
      fontSize: 32sp
      fontWeight: bold
      color: "#000000"

    body:
      fontSize: 14sp
      fontWeight: normal
      color: "#333333"

  spacing:
    small: 8dp
    medium: 16dp
    large: 24dp

  elevation:
    level1: 1dp
    level2: 2dp
    level3: 4dp
```

### 4.3 Theme Generation

**Command Execution:**

```typescript
// Generate Kotlin DSL
const result = await vscode.workspace.executeCommand(
    'magicui.generateTheme',
    'dsl',
    JSON.stringify(darkTheme)
);

console.log(result.output);
```

**Output (Kotlin DSL):**

```kotlin
object DarkTheme : Theme {
    override val colors = ColorScheme(
        primary = Color(0xFFBB86FC),
        primaryVariant = Color(0xFF3700B3),
        onPrimary = Color(0xFF000000),
        secondary = Color(0xFF03DAC6),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        error = Color(0xFFCF6679)
    )

    override val typography = Typography(
        h1 = TextStyle(
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        ),
        body = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFFCCCCCC)
        )
    )

    override val spacing = Spacing(
        small = 8.dp,
        medium = 16.dp,
        large = 24.dp
    )
}
```

---

## 5. Integration Examples

### 5.1 VS Code Extension Client

**File:** `vscode/src/extension.ts`

```typescript
import * as vscode from 'vscode';
import {
    LanguageClient,
    LanguageClientOptions,
    ServerOptions,
    TransportKind
} from 'vscode-languageclient/node';

let client: LanguageClient;

export function activate(context: vscode.ExtensionContext) {
    // Get JAR path from configuration
    const config = vscode.workspace.getConfiguration('magicui');
    const jarPath = config.get<string>('server.jarPath');

    if (!jarPath) {
        vscode.window.showErrorMessage(
            'MagicUI: Please configure magicui.server.jarPath in settings'
        );
        return;
    }

    // Server options
    const serverOptions: ServerOptions = {
        command: 'java',
        args: ['-jar', jarPath],
        transport: TransportKind.stdio
    };

    // Client options
    const clientOptions: LanguageClientOptions = {
        documentSelector: [
            { scheme: 'file', language: 'magicui-yaml' },
            { scheme: 'file', language: 'magicui-json' },
            { scheme: 'file', pattern: '**/*.magic.yaml' },
            { scheme: 'file', pattern: '**/*.magic.json' },
            { scheme: 'file', pattern: '**/*.magicui' }
        ],
        synchronize: {
            fileEvents: vscode.workspace.createFileSystemWatcher(
                '**/*.magic.{yaml,json}'
            )
        }
    };

    // Create and start client
    client = new LanguageClient(
        'magicui-lsp',
        'MagicUI Language Server',
        serverOptions,
        clientOptions
    );

    client.start();
}

export function deactivate(): Thenable<void> | undefined {
    if (!client) {
        return undefined;
    }
    return client.stop();
}
```

### 5.2 Custom Command Integration

```typescript
// Register custom command
vscode.commands.registerCommand('magicui.generateTheme', async () => {
    const editor = vscode.window.activeTextEditor;
    if (!editor) {
        vscode.window.showErrorMessage('No active editor');
        return;
    }

    const document = editor.document;
    const themeJson = document.getText();

    // Execute server command
    const result = await vscode.workspace.executeCommand(
        'magicui.generateTheme',
        'dsl',  // Format
        themeJson
    );

    // Show output
    const outputDoc = await vscode.workspace.openTextDocument({
        content: result.output,
        language: 'kotlin'
    });

    await vscode.window.showTextDocument(outputDoc);
});
```

### 5.3 Diagnostic Handling

```typescript
// Listen for diagnostics
client.onNotification('textDocument/publishDiagnostics', (params) => {
    const uri = params.uri;
    const diagnostics = params.diagnostics;

    console.log(`Diagnostics for ${uri}:`);
    diagnostics.forEach(diagnostic => {
        console.log(`  [${diagnostic.severity}] ${diagnostic.message}`);
    });

    // Show error count in status bar
    const errorCount = diagnostics.filter(
        d => d.severity === DiagnosticSeverity.Error
    ).length;

    const warningCount = diagnostics.filter(
        d => d.severity === DiagnosticSeverity.Warning
    ).length;

    vscode.window.setStatusBarMessage(
        `MagicUI: ${errorCount} errors, ${warningCount} warnings`
    );
});
```

---

## 6. Testing Examples

### 6.1 Document Lifecycle Test

```kotlin
@Test
fun `should handle document open, change, and close`() {
    val service = MagicUITextDocumentService()
    val mockClient = mockk<LanguageClient>(relaxed = true)
    service.connect(mockClient)

    val uri = "file:///test.magic.yaml"

    // Open document
    val openParams = DidOpenTextDocumentParams().apply {
        textDocument = TextDocumentItem().apply {
            this.uri = uri
            text = "Button:\n  vuid: test-btn"
        }
    }
    service.didOpen(openParams)

    verify { mockClient.publishDiagnostics(any()) }

    // Change document
    val changeParams = DidChangeTextDocumentParams().apply {
        textDocument = VersionedTextDocumentIdentifier().apply {
            this.uri = uri
            version = 2
        }
        contentChanges = listOf(
            TextDocumentContentChangeEvent().apply {
                text = "Button:\n  vuid: test-btn\n  text: Click"
            }
        )
    }
    service.didChange(changeParams)

    verify(atLeast = 2) { mockClient.publishDiagnostics(any()) }

    // Close document
    val closeParams = DidCloseTextDocumentParams().apply {
        textDocument = TextDocumentIdentifier(uri)
    }
    service.didClose(closeParams)

    // Document should be removed from cache
    assertTrue(true) // Document closed successfully
}
```

### 6.2 Completion Test

```kotlin
@Test
fun `should provide component completions`() {
    val service = MagicUITextDocumentService()
    val mockClient = mockk<LanguageClient>(relaxed = true)
    service.connect(mockClient)

    val uri = "file:///test.magic.yaml"
    val content = "B"  // User typed 'B'

    // Open document
    service.didOpen(DidOpenTextDocumentParams().apply {
        textDocument = TextDocumentItem().apply {
            this.uri = uri
            text = content
        }
    })

    // Request completion
    val completionParams = CompletionParams().apply {
        textDocument = TextDocumentIdentifier(uri)
        position = Position(0, 1)
    }

    val result = service.completion(completionParams).get()

    assertNotNull(result)
    val completions = result.right
    assertTrue(completions.items.size > 0)

    // Should suggest Button
    val buttonItem = completions.items.find { it.label == "Button" }
    assertNotNull(buttonItem)
    assertEquals(CompletionItemKind.Class, buttonItem.kind)
    assertTrue(buttonItem.insertText.contains("vuid:"))
}
```

### 6.3 Validation Test

```kotlin
@Test
fun `should validate button requires text or icon`() {
    val service = MagicUITextDocumentService()
    val mockClient = mockk<LanguageClient>(relaxed = true)
    service.connect(mockClient)

    val uri = "file:///test.magic.yaml"
    val content = """
        Button:
          vuid: broken-btn
          onClick: handleClick
    """.trimIndent()  // Missing text/icon

    service.didOpen(DidOpenTextDocumentParams().apply {
        textDocument = TextDocumentItem().apply {
            this.uri = uri
            text = content
        }
    })

    // Should publish diagnostics with warning
    verify {
        mockClient.publishDiagnostics(
            match { params ->
                params.diagnostics.any {
                    it.severity == DiagnosticSeverity.Warning &&
                    it.message.contains("Button should have 'text' or 'icon'")
                }
            }
        )
    }
}
```

### 6.4 Hover Test

```kotlin
@Test
fun `should provide hover documentation for components`() {
    val service = MagicUITextDocumentService()
    val mockClient = mockk<LanguageClient>(relaxed = true)
    service.connect(mockClient)

    val uri = "file:///test.magic.yaml"
    val content = "Button:\n  text: Click"

    service.didOpen(DidOpenTextDocumentParams().apply {
        textDocument = TextDocumentItem().apply {
            this.uri = uri
            text = content
        }
    })

    val hoverParams = HoverParams().apply {
        textDocument = TextDocumentIdentifier(uri)
        position = Position(0, 3)  // On "Button"
    }

    val result = service.hover(hoverParams).get()

    assertNotNull(result)
    assertNotNull(result.contents)

    val markdown = result.contents.right as MarkupContent
    assertEquals("markdown", markdown.kind)
    assertTrue(markdown.value.contains("Button Component"))
    assertTrue(markdown.value.contains("Interactive button"))
}
```

### 6.5 VUID Navigation Test

```kotlin
@Test
fun `should navigate to VUID definition`() {
    val service = MagicUITextDocumentService()
    val mockClient = mockk<LanguageClient>(relaxed = true)
    service.connect(mockClient)

    val uri = "file:///test.magic.yaml"
    val content = """
        Button:
          vuid: submit-button
          text: Submit
          onClick: submit-button
    """.trimIndent()

    service.didOpen(DidOpenTextDocumentParams().apply {
        textDocument = TextDocumentItem().apply {
            this.uri = uri
            text = content
        }
    })

    // Request definition for VUID reference in onClick
    val definitionParams = DefinitionParams().apply {
        textDocument = TextDocumentIdentifier(uri)
        position = Position(3, 20)  // On "submit-button" in onClick
    }

    val result = service.definition(definitionParams).get()

    assertNotNull(result)
    val locations = result.left
    assertTrue(locations.size > 0)

    val location = locations[0]
    assertEquals(uri, location.uri)
    assertEquals(1, location.range.start.line)  // Line with vuid declaration
}
```

### 6.6 Theme Generation Test

```kotlin
@Test
fun `should generate theme in multiple formats`() {
    val service = MagicUIWorkspaceService()
    val mockClient = mockk<LanguageClient>(relaxed = true)
    service.connect(mockClient)

    val themeJson = """
        {
            "name": "TestTheme",
            "colors": {
                "primary": "#FF0000",
                "secondary": "#00FF00"
            }
        }
    """.trimIndent()

    // Test DSL format
    val dslParams = ExecuteCommandParams().apply {
        command = "magicui.generateTheme"
        arguments = listOf("dsl", themeJson)
    }

    val dslResult = service.executeCommand(dslParams).get() as Map<*, *>
    assertEquals(true, dslResult["success"])
    assertEquals("TestTheme", dslResult["themeName"])
    assertNotNull(dslResult["output"])

    // Test YAML format
    val yamlParams = ExecuteCommandParams().apply {
        command = "magicui.generateTheme"
        arguments = listOf("yaml", themeJson)
    }

    val yamlResult = service.executeCommand(yamlParams).get() as Map<*, *>
    assertEquals(true, yamlResult["success"])

    // Test CSS format
    val cssParams = ExecuteCommandParams().apply {
        command = "magicui.generateTheme"
        arguments = listOf("css", themeJson)
    }

    val cssResult = service.executeCommand(cssParams).get() as Map<*, *>
    assertEquals(true, cssResult["success"])
}
```

---

## 7. Advanced Examples

### 7.1 Complex Form with Validation

```yaml
Screen:
  vuid: registration-screen
  title: Create Account
  children:
    - ScrollView:
        vuid: registration-scroll
        children:
          - Column:
              vuid: registration-form
              padding: 24dp
              children:
                - Text:
                    vuid: form-title
                    text: Create Account
                    fontSize: 24sp
                    fontWeight: bold
                    margin: 16dp

                - TextField:
                    vuid: reg-first-name
                    placeholder: First Name
                    onChange: handleFirstNameChange
                    validation:
                      required: true
                      minLength: 2
                      pattern: "^[A-Za-z]+$"
                      errorMessage: "First name must be at least 2 letters"

                - TextField:
                    vuid: reg-last-name
                    placeholder: Last Name
                    onChange: handleLastNameChange
                    validation:
                      required: true
                      minLength: 2

                - TextField:
                    vuid: reg-email
                    placeholder: Email Address
                    onChange: handleEmailChange
                    validation:
                      type: email
                      required: true
                      errorMessage: "Please enter a valid email address"

                - TextField:
                    vuid: reg-password
                    placeholder: Password
                    onChange: handlePasswordChange
                    secureTextEntry: true
                    validation:
                      type: password
                      minLength: 8
                      pattern: "^(?=.*[A-Z])(?=.*[0-9])"
                      errorMessage: "Password must be 8+ chars with uppercase and number"

                - TextField:
                    vuid: reg-confirm-password
                    placeholder: Confirm Password
                    onChange: handleConfirmPasswordChange
                    secureTextEntry: true
                    validation:
                      matches: reg-password
                      errorMessage: "Passwords must match"

                - Row:
                    vuid: terms-row
                    alignment: start
                    margin: 16dp
                    children:
                      - Checkbox:
                          vuid: terms-checkbox
                          checked: false
                          onChange: handleTermsChange

                      - Text:
                          vuid: terms-text
                          text: I agree to the Terms and Conditions
                          fontSize: 14sp

                - Button:
                    vuid: register-button
                    text: Create Account
                    onClick: handleRegister
                    backgroundColor: blue
                    color: white
                    width: 100%
                    height: 48dp
                    margin: 16dp
                    enabled: false  # Enabled when form is valid

                - Text:
                    vuid: login-link
                    text: Already have an account? Log in
                    fontSize: 14sp
                    color: blue
                    onClick: navigateToLogin
                    alignment: center
```

---

**End of Examples**

For comprehensive documentation, see:
- Developer Manual: `Docs/AVAMagic/Manuals/Developer/MagicUI-LSP-Developer-Manual-251224-V1.md`
- User Manual: `Docs/AVAMagic/Manuals/User/MagicUI-LSP-User-Manual-251224-V1.md`
- README: `Modules/AVAMagic/MagicTools/LanguageServer/README.md`
