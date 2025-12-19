# Live Preview Architecture - Multi-Platform Plan

**Version:** 1.0.0
**Date:** 2025-11-27
**Target Release:** v0.2.0 (Q1 2026)
**Platforms:** Android Studio, VSCode, Web Tool

---

## Executive Summary

This document outlines the comprehensive architecture for implementing live preview functionality across three development platforms: Android Studio plugin, VSCode extension, and standalone web tool. The live preview system will provide real-time visual feedback as developers write AVAMagic DSL code, with hot reload support and multi-platform rendering capabilities.

### Key Objectives

1. **Real-time Rendering:** Sub-100ms update latency from code change to visual update
2. **Multi-Platform Support:** Preview Android, iOS, Web, and Desktop renderings
3. **Hot Reload:** Instant visual updates without full recompilation
4. **Cross-IDE Consistency:** Identical preview experience across all three platforms
5. **Performance:** Handle files up to 10,000 lines without lag

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Android Studio Plugin](#android-studio-plugin)
3. [VSCode Extension](#vscode-extension)
4. [Web Tool](#web-tool)
5. [Shared Components](#shared-components)
6. [Implementation Phases](#implementation-phases)
7. [Technical Specifications](#technical-specifications)

---

## Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    AVAMagic DSL Code                        │
│                    (.ava files)                             │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────────┐
│              Parser & AST Generator                         │
│              (Shared across all platforms)                  │
└──────────────────┬──────────────────────────────────────────┘
                   │
      ┌────────────┼────────────┐
      │            │            │
      ▼            ▼            ▼
┌──────────┐ ┌──────────┐ ┌──────────┐
│ Android  │ │  VSCode  │ │   Web    │
│  Studio  │ │Extension │ │   Tool   │
└────┬─────┘ └────┬─────┘ └────┬─────┘
     │            │            │
     ▼            ▼            ▼
┌──────────────────────────────────────┐
│   Platform-Specific Renderers        │
│   - Android (Compose Preview)        │
│   - iOS (SwiftUI via Web)            │
│   - Web (React)                      │
│   - Desktop (Flutter Web)            │
└──────────────────────────────────────┘
```

### Core Principles

1. **Parser Reuse:** Single AST parser shared across all platforms
2. **Renderer Abstraction:** Platform-specific renderers implement common interface
3. **Hot Reload First:** Architecture optimized for incremental updates
4. **Web-Based Rendering:** Use web technologies for maximum compatibility
5. **Embedded WebView:** All three platforms use embedded browser for rendering

---

## Android Studio Plugin

### Overview

The Android Studio plugin will embed a JavaFX WebView component in a tool window, rendering the preview using web technologies. This approach provides maximum flexibility and consistency with other platforms.

### Architecture Components

```
┌─────────────────────────────────────────────┐
│      Android Studio IntelliJ Platform       │
├─────────────────────────────────────────────┤
│  ┌────────────────────────────────────┐    │
│  │   AVAMagic Preview Tool Window     │    │
│  │  ┌──────────────────────────────┐  │    │
│  │  │    JavaFX WebView            │  │    │
│  │  │  ┌────────────────────────┐  │  │    │
│  │  │  │   React Preview App    │  │  │    │
│  │  │  │ ┌────────────────────┐ │  │  │    │
│  │  │  │ │  Platform Selector│ │  │  │    │
│  │  │  │ └────────────────────┘ │  │  │    │
│  │  │  │ ┌────────────────────┐ │  │  │    │
│  │  │  │ │  Rendered Preview │ │  │  │    │
│  │  │  │ └────────────────────┘ │  │  │    │
│  │  │  └────────────────────────┘  │  │    │
│  │  └──────────────────────────────┘  │    │
│  └────────────────────────────────────┘    │
│                                             │
│  ┌────────────────────────────────────┐    │
│  │   File Change Listener             │    │
│  │   (PSI Tree Observer)              │    │
│  └──────────┬─────────────────────────┘    │
│             │                               │
│  ┌──────────▼─────────────────────────┐    │
│  │   AVA Parser (Kotlin/JVM)          │    │
│  │   - Lexer                          │    │
│  │   - Parser                         │    │
│  │   - AST Generator                  │    │
│  └──────────┬─────────────────────────┘    │
│             │                               │
│  ┌──────────▼─────────────────────────┐    │
│  │   WebView Bridge                   │    │
│  │   - Send AST updates               │    │
│  │   - Receive events                 │    │
│  └────────────────────────────────────┘    │
└─────────────────────────────────────────────┘
```

### Implementation Details

#### 1. Tool Window Setup

**Location:** `tools/android-studio-plugin/src/main/kotlin/preview/`

**Key Files:**
- `PreviewToolWindowFactory.kt` - Creates tool window
- `PreviewPanel.kt` - Main panel with WebView
- `WebViewManager.kt` - Manages JavaFX WebView
- `PreviewState.kt` - Manages preview state (platform selection, zoom, etc.)

**Code Structure:**
```kotlin
// PreviewToolWindowFactory.kt
class PreviewToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow
    ) {
        val contentFactory = ContentFactory.getInstance()
        val previewPanel = PreviewPanel(project)
        val content = contentFactory.createContent(
            previewPanel,
            "Preview",
            false
        )
        toolWindow.contentManager.addContent(content)
    }
}

// PreviewPanel.kt
class PreviewPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val webViewManager = WebViewManager()
    private val parser = AvaParser()

    init {
        setupUI()
        setupFileListener()
    }

    private fun setupUI() {
        // Toolbar with platform selector
        val toolbar = createToolbar()
        add(toolbar, BorderLayout.NORTH)

        // WebView component
        add(webViewManager.component, BorderLayout.CENTER)
    }

    private fun setupFileListener() {
        // Listen to .ava file changes
        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    handleFileChange(event.newFile)
                }
            }
        )
    }
}
```

#### 2. File Change Detection

**Approach:** Use IntelliJ PSI (Program Structure Interface) tree observation

**Implementation:**
```kotlin
class AvaFileChangeListener(
    private val project: Project,
    private val onAstUpdate: (ASTNode) -> Unit
) {
    private val psiManager = PsiManager.getInstance(project)

    fun startListening() {
        psiManager.addPsiTreeChangeListener(object : PsiTreeChangeAdapter() {
            override fun childrenChanged(event: PsiTreeChangeEvent) {
                val file = event.file
                if (file?.fileType?.name == "AVA") {
                    debounce {
                        parseAndUpdate(file)
                    }
                }
            }
        })
    }

    private fun parseAndUpdate(file: PsiFile) {
        runReadAction {
            val text = file.text
            val ast = AvaParser.parse(text)
            onAstUpdate(ast)
        }
    }

    // Debounce to avoid excessive parsing
    private fun debounce(delay: Long = 150, action: () -> Unit) {
        debounceTimer?.cancel()
        debounceTimer = Timer(delay) { action() }
        debounceTimer?.start()
    }
}
```

#### 3. WebView Bridge

**Communication:** Kotlin ↔ JavaScript via `webEngine.executeScript()` and `upcallHandler`

**Implementation:**
```kotlin
class WebViewBridge(private val webView: WebView) {
    private val webEngine = webView.engine

    init {
        setupBridge()
    }

    private fun setupBridge() {
        // Inject bridge object
        webEngine.loadWorker.stateProperty().addListener { _, _, newState ->
            if (newState == Worker.State.SUCCEEDED) {
                webEngine.executeScript("""
                    window.androidStudioBridge = {
                        log: function(msg) { console.log(msg); },
                        updateAST: null  // Will be set from Kotlin
                    };
                """)
            }
        }
    }

    fun sendASTUpdate(ast: JSONObject) {
        Platform.runLater {
            val astJson = ast.toString().replace("'", "\\'")
            webEngine.executeScript("""
                if (window.previewApp && window.previewApp.updateAST) {
                    window.previewApp.updateAST($astJson);
                }
            """)
        }
    }

    fun loadPreviewApp(url: String) {
        Platform.runLater {
            webEngine.load(url)
        }
    }
}
```

#### 4. Parser Integration

**Location:** Reuse existing parser from VSCode extension, port to Kotlin

**Implementation Strategy:**
1. **Option A:** Call TypeScript parser via Node.js subprocess
2. **Option B:** Port parser to Kotlin (recommended for performance)
3. **Option C:** Use GraalVM to run TypeScript parser directly in JVM

**Recommended: Option B - Kotlin Parser**

```kotlin
// Port from TypeScript parser
class AvaLexer(private val source: String) {
    private var position = 0
    private var line = 1
    private var column = 1

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        while (!isAtEnd()) {
            val token = scanToken()
            if (token != null) tokens.add(token)
        }
        tokens.add(Token(TokenType.EOF, "", line, column))
        return tokens
    }

    // ... rest of lexer implementation
}

class AvaParser(private val tokens: List<Token>) {
    private var current = 0

    fun parse(): DocumentNode {
        val screens = mutableListOf<ScreenNode>()
        while (!isAtEnd()) {
            screens.add(parseScreen())
        }
        return DocumentNode(screens)
    }

    // ... rest of parser implementation
}
```

### Platform Selection

**UI Component:** Dropdown in toolbar

**Options:**
- Android (Jetpack Compose)
- iOS (SwiftUI simulation)
- Web (React)
- Desktop (Flutter Web)

**Implementation:**
```kotlin
class PlatformSelector : JComboBox<Platform>() {
    init {
        model = DefaultComboBoxModel(Platform.values())
        addActionListener {
            onPlatformChange(selectedItem as Platform)
        }
    }

    private fun onPlatformChange(platform: Platform) {
        // Notify WebView to switch renderer
        webViewBridge.setPlatform(platform.name.lowercase())
    }
}

enum class Platform {
    ANDROID,
    IOS,
    WEB,
    DESKTOP
}
```

### Hot Reload Implementation

**Mechanism:** Incremental AST diff + React state update

**Flow:**
1. User types in editor
2. PSI tree change detected (debounced 150ms)
3. Parser generates new AST
4. AST diff computed (compare old vs new)
5. Only changed nodes sent to WebView
6. React re-renders affected components

**AST Diff Algorithm:**
```kotlin
class ASTDiffer {
    fun diff(oldAST: DocumentNode, newAST: DocumentNode): List<ASTChange> {
        val changes = mutableListOf<ASTChange>()

        // Compare screens
        val oldScreens = oldAST.screens.associateBy { it.name }
        val newScreens = newAST.screens.associateBy { it.name }

        // Detect additions
        newScreens.forEach { (name, screen) ->
            if (!oldScreens.containsKey(name)) {
                changes.add(ASTChange.Add(screen))
            } else {
                // Deep compare
                val oldScreen = oldScreens[name]!!
                if (screen != oldScreen) {
                    changes.add(ASTChange.Update(screen))
                }
            }
        }

        // Detect deletions
        oldScreens.forEach { (name, _) ->
            if (!newScreens.containsKey(name)) {
                changes.add(ASTChange.Delete(name))
            }
        }

        return changes
    }
}

sealed class ASTChange {
    data class Add(val screen: ScreenNode) : ASTChange()
    data class Update(val screen: ScreenNode) : ASTChange()
    data class Delete(val screenName: String) : ASTChange()
}
```

---

## VSCode Extension

### Overview

The VSCode extension will use a WebView panel to render the preview. This is already partially implemented in the extension architecture, so we'll extend the existing webview infrastructure.

### Architecture Components

```
┌─────────────────────────────────────────────┐
│           VSCode Extension Host              │
├─────────────────────────────────────────────┤
│  ┌────────────────────────────────────┐    │
│  │   Preview WebView Panel            │    │
│  │  ┌──────────────────────────────┐  │    │
│  │  │   React Preview App          │  │    │
│  │  │  (Same as Android Studio)    │  │    │
│  │  └──────────────────────────────┘  │    │
│  └────────────────────────────────────┘    │
│                                             │
│  ┌────────────────────────────────────┐    │
│  │   Document Change Listener         │    │
│  │   (vscode.workspace.onDidChange    │    │
│  │    TextDocument)                   │    │
│  └──────────┬─────────────────────────┘    │
│             │                               │
│  ┌──────────▼─────────────────────────┐    │
│  │   AVA Parser (TypeScript)          │    │
│  │   (Existing implementation)        │    │
│  └──────────┬─────────────────────────┘    │
│             │                               │
│  ┌──────────▼─────────────────────────┐    │
│  │   WebView Message Passing          │    │
│  │   - postMessage(AST)               │    │
│  │   - onMessage(events)              │    │
│  └────────────────────────────────────┘    │
└─────────────────────────────────────────────┘
```

### Implementation Details

#### 1. Preview Panel Command

**Location:** `tools/vscode-extension/src/extension/PreviewPanel.ts`

**Command Registration:**
```typescript
// src/extension.ts
export function activate(context: vscode.ExtensionContext) {
    context.subscriptions.push(
        vscode.commands.registerCommand('avamagic.showPreview', () => {
            PreviewPanel.createOrShow(context.extensionUri);
        })
    );

    // Auto-open preview when .ava file is active
    vscode.window.onDidChangeActiveTextEditor(editor => {
        if (editor?.document.languageId === 'ava') {
            PreviewPanel.updateContent(editor.document);
        }
    });
}
```

#### 2. WebView Panel Implementation

**Code:**
```typescript
// PreviewPanel.ts
export class PreviewPanel {
    public static currentPanel: PreviewPanel | undefined;
    private readonly _panel: vscode.WebviewPanel;
    private readonly _extensionUri: vscode.Uri;
    private _disposables: vscode.Disposable[] = [];
    private _parser: AvaParser;

    public static createOrShow(extensionUri: vscode.Uri) {
        const column = vscode.window.activeTextEditor
            ? vscode.window.activeTextEditor.viewColumn
            : undefined;

        // If we already have a panel, show it
        if (PreviewPanel.currentPanel) {
            PreviewPanel.currentPanel._panel.reveal(column);
            return;
        }

        // Otherwise, create a new panel
        const panel = vscode.window.createWebviewPanel(
            'avaMagicPreview',
            'AVAMagic Preview',
            column || vscode.ViewColumn.Two,
            {
                enableScripts: true,
                retainContextWhenHidden: true,
                localResourceRoots: [
                    vscode.Uri.joinPath(extensionUri, 'dist'),
                    vscode.Uri.joinPath(extensionUri, 'assets')
                ]
            }
        );

        PreviewPanel.currentPanel = new PreviewPanel(panel, extensionUri);
    }

    private constructor(panel: vscode.WebviewPanel, extensionUri: vscode.Uri) {
        this._panel = panel;
        this._extensionUri = extensionUri;
        this._parser = new AvaParser();

        // Set the webview's initial html content
        this._update();

        // Listen for when the panel is disposed
        this._panel.onDidDispose(() => this.dispose(), null, this._disposables);

        // Handle messages from the webview
        this._panel.webview.onDidReceiveMessage(
            message => {
                switch (message.command) {
                    case 'alert':
                        vscode.window.showInformationMessage(message.text);
                        return;
                    case 'setPlatform':
                        this.handlePlatformChange(message.platform);
                        return;
                }
            },
            null,
            this._disposables
        );

        // Listen to document changes
        vscode.workspace.onDidChangeTextDocument(
            e => this.handleDocumentChange(e),
            null,
            this._disposables
        );
    }

    private handleDocumentChange(e: vscode.TextDocumentChangeEvent) {
        if (e.document.languageId !== 'ava') return;

        // Debounce updates
        clearTimeout(this._updateTimeout);
        this._updateTimeout = setTimeout(() => {
            this.updatePreview(e.document);
        }, 150);
    }

    private updatePreview(document: vscode.TextDocument) {
        const text = document.getText();
        const parseResult = this._parser.parse(text);

        // Send AST to webview
        this._panel.webview.postMessage({
            command: 'updateAST',
            ast: parseResult.ast,
            errors: parseResult.errors
        });
    }

    private _update() {
        const webview = this._panel.webview;
        this._panel.webview.html = this._getHtmlForWebview(webview);
    }

    private _getHtmlForWebview(webview: vscode.Webview): string {
        // Get path to preview app bundle
        const scriptUri = webview.asWebviewUri(
            vscode.Uri.joinPath(this._extensionUri, 'dist', 'preview.js')
        );

        const styleUri = webview.asWebviewUri(
            vscode.Uri.joinPath(this._extensionUri, 'dist', 'preview.css')
        );

        // Use a nonce to whitelist which scripts can be run
        const nonce = getNonce();

        return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="Content-Security-Policy"
          content="default-src 'none';
                   style-src ${webview.cspSource} 'unsafe-inline';
                   script-src 'nonce-${nonce}';">
    <link href="${styleUri}" rel="stylesheet">
    <title>AVAMagic Preview</title>
</head>
<body>
    <div id="root"></div>
    <script nonce="${nonce}" src="${scriptUri}"></script>
</body>
</html>`;
    }

    public dispose() {
        PreviewPanel.currentPanel = undefined;

        this._panel.dispose();

        while (this._disposables.length) {
            const disposable = this._disposables.pop();
            if (disposable) {
                disposable.dispose();
            }
        }
    }
}

function getNonce() {
    let text = '';
    const possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    for (let i = 0; i < 32; i++) {
        text += possible.charAt(Math.floor(Math.random() * possible.length));
    }
    return text;
}
```

#### 3. Preview React App

**Location:** `tools/vscode-extension/src/webviews/preview/`

**Entry Point:**
```tsx
// index.tsx
import React from 'react';
import ReactDOM from 'react-dom';
import { PreviewApp } from './PreviewApp';

// Get VS Code API
declare const acquireVsCodeApi: any;
const vscode = acquireVsCodeApi();

// Mount React app
ReactDOM.render(
    <PreviewApp vscode={vscode} />,
    document.getElementById('root')
);

// Expose to window for extension
(window as any).previewApp = {
    updateAST: (ast: any) => {
        // Trigger React state update
        window.dispatchEvent(new CustomEvent('ast-update', { detail: ast }));
    }
};
```

**Preview App Component:**
```tsx
// PreviewApp.tsx
import React, { useState, useEffect } from 'react';
import { PlatformSelector } from './components/PlatformSelector';
import { AndroidRenderer } from './renderers/AndroidRenderer';
import { IOSRenderer } from './renderers/IOSRenderer';
import { WebRenderer } from './renderers/WebRenderer';
import { DesktopRenderer } from './renderers/DesktopRenderer';

export const PreviewApp: React.FC<{ vscode: any }> = ({ vscode }) => {
    const [platform, setPlatform] = useState<'android' | 'ios' | 'web' | 'desktop'>('android');
    const [ast, setAST] = useState<any>(null);
    const [zoom, setZoom] = useState(100);

    useEffect(() => {
        // Listen for AST updates from extension
        const handleASTUpdate = (event: CustomEvent) => {
            setAST(event.detail);
        };

        window.addEventListener('ast-update', handleASTUpdate as any);

        // Listen for messages from extension
        window.addEventListener('message', (event) => {
            const message = event.data;
            switch (message.command) {
                case 'updateAST':
                    setAST(message.ast);
                    break;
                case 'setPlatform':
                    setPlatform(message.platform);
                    break;
            }
        });

        return () => {
            window.removeEventListener('ast-update', handleASTUpdate as any);
        };
    }, []);

    const handlePlatformChange = (newPlatform: string) => {
        setPlatform(newPlatform as any);
        vscode.postMessage({
            command: 'setPlatform',
            platform: newPlatform
        });
    };

    const renderPreview = () => {
        if (!ast) {
            return <div className="empty-state">Open a .ava file to see preview</div>;
        }

        switch (platform) {
            case 'android':
                return <AndroidRenderer ast={ast} zoom={zoom} />;
            case 'ios':
                return <IOSRenderer ast={ast} zoom={zoom} />;
            case 'web':
                return <WebRenderer ast={ast} zoom={zoom} />;
            case 'desktop':
                return <DesktopRenderer ast={ast} zoom={zoom} />;
        }
    };

    return (
        <div className="preview-app">
            <div className="toolbar">
                <PlatformSelector
                    value={platform}
                    onChange={handlePlatformChange}
                />
                <div className="zoom-controls">
                    <button onClick={() => setZoom(z => Math.max(25, z - 25))}>-</button>
                    <span>{zoom}%</span>
                    <button onClick={() => setZoom(z => Math.min(200, z + 25))}>+</button>
                </div>
            </div>
            <div className="preview-container" style={{ transform: `scale(${zoom / 100})` }}>
                {renderPreview()}
            </div>
        </div>
    );
};
```

#### 4. Hot Reload

**Implementation:** Use React's state management + AST diffing

**Performance Optimization:**
```typescript
// Use React.memo to prevent unnecessary re-renders
export const AndroidRenderer = React.memo(({ ast, zoom }) => {
    // Only re-render when AST actually changes
    return <div>...</div>;
}, (prevProps, nextProps) => {
    // Custom comparison
    return deepEqual(prevProps.ast, nextProps.ast) && prevProps.zoom === nextProps.zoom;
});
```

---

## Web Tool

### Overview

Standalone web application for AVAMagic preview, deployable to any hosting platform. Can be used independently or embedded in other tools.

### Architecture Components

```
┌─────────────────────────────────────────────┐
│        Web Application (React)               │
├─────────────────────────────────────────────┤
│  ┌────────────────────────────────────┐    │
│  │   Monaco Editor (Code Editor)      │    │
│  │   - Syntax highlighting            │    │
│  │   - Auto-completion                │    │
│  │   - Error markers                  │    │
│  └──────────┬─────────────────────────┘    │
│             │                               │
│  ┌──────────▼─────────────────────────┐    │
│  │   AVA Parser (TypeScript/WASM)     │    │
│  │   - Runs in browser                │    │
│  │   - Web Worker for performance     │    │
│  └──────────┬─────────────────────────┘    │
│             │                               │
│  ┌──────────▼─────────────────────────┐    │
│  │   Preview Panel                    │    │
│  │   (Same React components)          │    │
│  └────────────────────────────────────┘    │
└─────────────────────────────────────────────┘
```

### Implementation Details

#### 1. Project Structure

```
tools/web-preview/
├── public/
│   ├── index.html
│   └── assets/
├── src/
│   ├── index.tsx                    # Entry point
│   ├── App.tsx                      # Main app component
│   ├── components/
│   │   ├── Editor.tsx               # Monaco editor wrapper
│   │   ├── Preview.tsx              # Preview panel
│   │   ├── Toolbar.tsx              # Platform selector, etc.
│   │   └── SplitView.tsx            # Resizable split pane
│   ├── parser/
│   │   ├── worker.ts                # Web Worker for parsing
│   │   ├── parser.ts                # Parser (reused from VSCode)
│   │   └── lexer.ts                 # Lexer (reused from VSCode)
│   ├── renderers/
│   │   ├── AndroidRenderer.tsx      # Shared with VSCode
│   │   ├── IOSRenderer.tsx
│   │   ├── WebRenderer.tsx
│   │   └── DesktopRenderer.tsx
│   └── utils/
│       ├── storage.ts               # LocalStorage for code
│       └── themes.ts                # Editor themes
├── package.json
├── tsconfig.json
└── vite.config.ts                   # Vite for bundling
```

#### 2. Main App Component

```tsx
// App.tsx
import React, { useState, useCallback } from 'react';
import { Editor } from './components/Editor';
import { Preview } from './components/Preview';
import { Toolbar } from './components/Toolbar';
import { SplitView } from './components/SplitView';
import { parseAva } from './parser/worker';

export const App: React.FC = () => {
    const [code, setCode] = useState(loadFromLocalStorage('lastCode') || DEFAULT_CODE);
    const [ast, setAST] = useState<any>(null);
    const [errors, setErrors] = useState<any[]>([]);
    const [platform, setPlatform] = useState<Platform>('android');
    const [isLoading, setIsLoading] = useState(false);

    const handleCodeChange = useCallback(async (newCode: string) => {
        setCode(newCode);
        saveToLocalStorage('lastCode', newCode);

        setIsLoading(true);
        try {
            const result = await parseAva(newCode);
            setAST(result.ast);
            setErrors(result.errors);
        } catch (error) {
            console.error('Parse error:', error);
            setErrors([{ message: error.message }]);
        } finally {
            setIsLoading(false);
        }
    }, []);

    return (
        <div className="app">
            <Toolbar
                platform={platform}
                onPlatformChange={setPlatform}
                onShare={() => shareCode(code)}
                onExport={() => exportCode(ast, platform)}
            />
            <SplitView
                left={
                    <Editor
                        value={code}
                        onChange={handleCodeChange}
                        errors={errors}
                        isLoading={isLoading}
                    />
                }
                right={
                    <Preview
                        ast={ast}
                        platform={platform}
                        errors={errors}
                    />
                }
            />
        </div>
    );
};

const DEFAULT_CODE = `Screen "MyApp" {
    AppBar(title: "Hello AVAMagic")

    Column(spacing: 16.dp, padding: 16.dp) {
        Text("Welcome to AVAMagic Live Preview!")
        Button("Click Me", onClick: handleClick)
    }
}`;
```

#### 3. Monaco Editor Integration

```tsx
// components/Editor.tsx
import React, { useRef, useEffect } from 'react';
import * as monaco from 'monaco-editor';

export const Editor: React.FC<{
    value: string;
    onChange: (value: string) => void;
    errors: ParseError[];
    isLoading: boolean;
}> = ({ value, onChange, errors, isLoading }) => {
    const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);
    const containerRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (!containerRef.current) return;

        // Register AVA language
        monaco.languages.register({ id: 'ava' });

        // Configure syntax highlighting
        monaco.languages.setMonarchTokensProvider('ava', AVA_LANGUAGE_DEFINITION);

        // Create editor
        editorRef.current = monaco.editor.create(containerRef.current, {
            value,
            language: 'ava',
            theme: 'vs-dark',
            minimap: { enabled: false },
            fontSize: 14,
            lineNumbers: 'on',
            automaticLayout: true,
        });

        // Listen to changes
        editorRef.current.onDidChangeModelContent(() => {
            const newValue = editorRef.current!.getValue();
            onChange(newValue);
        });

        return () => {
            editorRef.current?.dispose();
        };
    }, []);

    // Update error markers
    useEffect(() => {
        if (!editorRef.current) return;

        const model = editorRef.current.getModel();
        if (!model) return;

        const markers = errors.map(err => ({
            severity: monaco.MarkerSeverity.Error,
            message: err.message,
            startLineNumber: err.line,
            startColumn: err.column,
            endLineNumber: err.line,
            endColumn: err.column + 10
        }));

        monaco.editor.setModelMarkers(model, 'ava', markers);
    }, [errors]);

    return (
        <div className="editor-container">
            {isLoading && <div className="loading-overlay">Parsing...</div>}
            <div ref={containerRef} className="editor" />
        </div>
    );
};

// Language definition for syntax highlighting
const AVA_LANGUAGE_DEFINITION = {
    keywords: ['Screen', 'if', 'else', 'for', 'while'],
    typeKeywords: [],
    operators: ['=', ':', ',', '{', '}', '(', ')', '.'],

    tokenizer: {
        root: [
            [/[a-z_$][\w$]*/, {
                cases: {
                    '@keywords': 'keyword',
                    '@default': 'identifier'
                }
            }],
            [/[A-Z][\w$]*/, 'type.identifier'],
            [/"([^"\\]|\\.)*$/, 'string.invalid'],
            [/"/, 'string', '@string'],
            [/\d+(\.\d+)?/, 'number'],
            [/[{}()\[\]]/, '@brackets'],
        ],

        string: [
            [/[^\\"]+/, 'string'],
            [/"/, 'string', '@pop']
        ],
    },
};
```

#### 4. Web Worker for Parsing

**Performance:** Offload parsing to Web Worker to keep UI responsive

```typescript
// parser/worker.ts
import { AvaParser } from './parser';

let parser: AvaParser | null = null;

self.addEventListener('message', (event) => {
    const { id, command, code } = event.data;

    switch (command) {
        case 'init':
            parser = new AvaParser();
            self.postMessage({ id, result: 'initialized' });
            break;

        case 'parse':
            if (!parser) {
                self.postMessage({ id, error: 'Parser not initialized' });
                return;
            }

            try {
                const result = parser.parse(code);
                self.postMessage({ id, result });
            } catch (error) {
                self.postMessage({ id, error: error.message });
            }
            break;
    }
});

// Wrapper function for main thread
export async function parseAva(code: string): Promise<ParseResult> {
    return new Promise((resolve, reject) => {
        const worker = new Worker(new URL('./worker.ts', import.meta.url), {
            type: 'module'
        });

        const id = Math.random().toString(36);

        worker.addEventListener('message', (event) => {
            if (event.data.id === id) {
                if (event.data.error) {
                    reject(new Error(event.data.error));
                } else {
                    resolve(event.data.result);
                }
                worker.terminate();
            }
        });

        worker.postMessage({ id, command: 'parse', code });
    });
}
```

#### 5. Deployment

**Hosting Options:**
1. **Vercel:** Automatic deployments from Git
2. **Netlify:** CDN distribution
3. **GitHub Pages:** Free hosting
4. **Self-hosted:** Nginx/Apache

**Build Configuration:**
```json
// package.json
{
    "scripts": {
        "dev": "vite",
        "build": "vite build",
        "preview": "vite preview",
        "deploy": "vite build && vercel --prod"
    }
}
```

**Vite Configuration:**
```typescript
// vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
    plugins: [react()],
    base: '/avamagic-preview/',  // For GitHub Pages
    build: {
        outDir: 'dist',
        sourcemap: true,
        rollupOptions: {
            output: {
                manualChunks: {
                    'monaco': ['monaco-editor'],
                    'react': ['react', 'react-dom'],
                }
            }
        }
    },
    optimizeDeps: {
        include: ['monaco-editor']
    }
});
```

---

## Shared Components

### Component Renderers

All three platforms share the same React renderer components. These are built once and reused everywhere.

#### Renderer Interface

```typescript
interface RendererProps {
    ast: DocumentNode;
    zoom: number;
    onError?: (error: Error) => void;
}

interface ComponentRenderer {
    renderScreen(screen: ScreenNode): React.ReactElement;
    renderComponent(component: ComponentNode): React.ReactElement;
    renderProperty(property: PropertyNode): any;
}
```

#### Android Renderer (Compose Simulation)

```tsx
// renderers/AndroidRenderer.tsx
import React from 'react';
import { DocumentNode, ScreenNode, ComponentNode } from '../parser/ast';

export const AndroidRenderer: React.FC<RendererProps> = ({ ast, zoom }) => {
    return (
        <div className="android-preview" style={{ transform: `scale(${zoom / 100})` }}>
            <div className="android-device-frame">
                {ast.screens.map(screen => (
                    <AndroidScreen key={screen.name} screen={screen} />
                ))}
            </div>
        </div>
    );
};

const AndroidScreen: React.FC<{ screen: ScreenNode }> = ({ screen }) => {
    return (
        <div className="compose-screen">
            {screen.children.map((component, idx) => (
                <AndroidComponent key={idx} component={component} />
            ))}
        </div>
    );
};

const AndroidComponent: React.FC<{ component: ComponentNode }> = ({ component }) => {
    // Map AVAMagic components to Android Compose equivalents
    switch (component.name) {
        case 'AppBar':
            return <ComposeAppBar {...getProps(component)} />;
        case 'Column':
            return <ComposeColumn {...getProps(component)}>
                {component.children?.map((child, idx) => (
                    <AndroidComponent key={idx} component={child} />
                ))}
            </ComposeColumn>;
        case 'Text':
            return <ComposeText {...getProps(component)} />;
        case 'Button':
            return <ComposeButton {...getProps(component)} />;
        // ... more components
        default:
            return <div className="unknown-component">{component.name}</div>;
    }
};

// Compose component implementations
const ComposeAppBar: React.FC<any> = ({ title }) => (
    <div className="compose-app-bar">
        <h1>{title}</h1>
    </div>
);

const ComposeColumn: React.FC<any> = ({ spacing, padding, children }) => (
    <div
        className="compose-column"
        style={{
            display: 'flex',
            flexDirection: 'column',
            gap: spacing,
            padding: padding
        }}
    >
        {children}
    </div>
);

// ... more Compose components
```

#### iOS Renderer (SwiftUI Simulation)

```tsx
// renderers/IOSRenderer.tsx
import React from 'react';

export const IOSRenderer: React.FC<RendererProps> = ({ ast, zoom }) => {
    return (
        <div className="ios-preview" style={{ transform: `scale(${zoom / 100})` }}>
            <div className="ios-device-frame">
                {ast.screens.map(screen => (
                    <SwiftUIScreen key={screen.name} screen={screen} />
                ))}
            </div>
        </div>
    );
};

const SwiftUIScreen: React.FC<{ screen: ScreenNode }> = ({ screen }) => {
    return (
        <div className="swiftui-screen">
            {screen.children.map((component, idx) => (
                <SwiftUIComponent key={idx} component={component} />
            ))}
        </div>
    );
};

const SwiftUIComponent: React.FC<{ component: ComponentNode }> = ({ component }) => {
    switch (component.name) {
        case 'AppBar':
            return <SwiftUINavigationBar {...getProps(component)} />;
        case 'Column':
            return <SwiftUIVStack {...getProps(component)}>
                {component.children?.map((child, idx) => (
                    <SwiftUIComponent key={idx} component={child} />
                ))}
            </SwiftUIVStack>;
        case 'Text':
            return <SwiftUIText {...getProps(component)} />;
        case 'Button':
            return <SwiftUIButton {...getProps(component)} />;
        default:
            return <div className="unknown-component">{component.name}</div>;
    }
};

// SwiftUI component implementations
const SwiftUINavigationBar: React.FC<any> = ({ title }) => (
    <div className="swiftui-navigation-bar">
        <h1>{title}</h1>
    </div>
);

const SwiftUIVStack: React.FC<any> = ({ spacing, padding, children }) => (
    <div
        className="swiftui-vstack"
        style={{
            display: 'flex',
            flexDirection: 'column',
            gap: spacing,
            padding: padding
        }}
    >
        {children}
    </div>
);

// ... more SwiftUI components
```

#### Web Renderer (React)

```tsx
// renderers/WebRenderer.tsx
import React from 'react';

export const WebRenderer: React.FC<RendererProps> = ({ ast, zoom }) => {
    return (
        <div className="web-preview" style={{ transform: `scale(${zoom / 100})` }}>
            <div className="browser-frame">
                {ast.screens.map(screen => (
                    <ReactScreen key={screen.name} screen={screen} />
                ))}
            </div>
        </div>
    );
};

const ReactScreen: React.FC<{ screen: ScreenNode }> = ({ screen }) => {
    return (
        <div className="react-screen">
            {screen.children.map((component, idx) => (
                <ReactComponent key={idx} component={component} />
            ))}
        </div>
    );
};

const ReactComponent: React.FC<{ component: ComponentNode }> = ({ component }) => {
    switch (component.name) {
        case 'AppBar':
            return <ReactAppBar {...getProps(component)} />;
        case 'Column':
            return <div
                className="react-column"
                style={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: getProps(component).spacing,
                    padding: getProps(component).padding
                }}
            >
                {component.children?.map((child, idx) => (
                    <ReactComponent key={idx} component={child} />
                ))}
            </div>;
        case 'Text':
            return <p className="react-text">{getProps(component).content}</p>;
        case 'Button':
            return <button className="react-button">{getProps(component).text}</button>;
        default:
            return <div className="unknown-component">{component.name}</div>;
    }
};

// ... React component implementations
```

### Property Mapping Utility

```typescript
// utils/propertyMapper.ts
export function getProps(component: ComponentNode): Record<string, any> {
    const props: Record<string, any> = {};

    component.properties.forEach(prop => {
        props[prop.key] = extractValue(prop.value);
    });

    return props;
}

function extractValue(valueNode: ValueNode): any {
    switch (valueNode.type) {
        case 'StringValue':
            return valueNode.value;
        case 'NumberValue':
            // Handle units like dp, sp, px
            if (valueNode.unit) {
                return `${valueNode.value}${valueNode.unit}`;
            }
            return valueNode.value;
        case 'BooleanValue':
            return valueNode.value;
        case 'VariableValue':
            // For preview, use placeholder values
            return `$${valueNode.name}`;
        default:
            return null;
    }
}
```

---

## Implementation Phases

### Phase 1: Foundation (Weeks 1-2)

**Goals:**
- Set up shared renderer components
- Implement basic AST-to-React rendering
- Create platform selector UI

**Deliverables:**
1. Shared React renderer components for all 4 platforms
2. Basic component mapping (10-15 core components)
3. Property extraction and mapping utility
4. Platform selector dropdown

**Tasks:**
- [ ] Create `renderers/` directory structure
- [ ] Implement `AndroidRenderer.tsx` (basic)
- [ ] Implement `IOSRenderer.tsx` (basic)
- [ ] Implement `WebRenderer.tsx` (basic)
- [ ] Implement `DesktopRenderer.tsx` (basic)
- [ ] Create `PlatformSelector` component
- [ ] Add CSS for device frames
- [ ] Write unit tests for renderers

### Phase 2: VSCode Integration (Weeks 3-4)

**Goals:**
- Integrate preview panel into VSCode extension
- Implement document change listener
- Add hot reload support

**Deliverables:**
1. VSCode WebView panel with preview
2. Real-time code-to-preview synchronization
3. Hot reload with <150ms latency

**Tasks:**
- [ ] Create `PreviewPanel.ts` in VSCode extension
- [ ] Set up WebView communication
- [ ] Integrate existing parser
- [ ] Implement document change listener with debouncing
- [ ] Build React preview app for WebView
- [ ] Add webpack configuration for preview bundle
- [ ] Test hot reload performance
- [ ] Add error handling and user feedback

### Phase 3: Android Studio Plugin (Weeks 5-7)

**Goals:**
- Port preview functionality to Android Studio
- Implement JavaFX WebView integration
- Add PSI tree observation

**Deliverables:**
1. Android Studio tool window with preview
2. Kotlin/JVM parser (or Node.js bridge)
3. Hot reload for .ava files

**Tasks:**
- [ ] Create Android Studio plugin structure
- [ ] Implement `PreviewToolWindowFactory.kt`
- [ ] Set up JavaFX WebView component
- [ ] Port TypeScript parser to Kotlin (or use Node bridge)
- [ ] Implement PSI tree change listener
- [ ] Create WebView bridge (Kotlin ↔ JavaScript)
- [ ] Package React preview app for plugin
- [ ] Add platform selector and zoom controls
- [ ] Test with IntelliJ 2024.1+

### Phase 4: Web Tool (Weeks 8-9)

**Goals:**
- Create standalone web application
- Add Monaco editor integration
- Deploy to hosting platform

**Deliverables:**
1. Standalone web preview tool
2. Monaco editor with AVA syntax highlighting
3. Deployed to Vercel/Netlify

**Tasks:**
- [ ] Set up Vite + React project
- [ ] Integrate Monaco editor
- [ ] Add AVA language definition for Monaco
- [ ] Implement Web Worker for parsing
- [ ] Create split-view layout
- [ ] Add localStorage for code persistence
- [ ] Implement share/export functionality
- [ ] Configure Vite build
- [ ] Deploy to Vercel
- [ ] Add analytics (optional)

### Phase 5: Advanced Features (Weeks 10-12)

**Goals:**
- Expand component library coverage
- Add interaction simulation
- Implement error highlighting

**Deliverables:**
1. 50+ components fully rendered
2. Click/hover interaction simulation
3. Inline error markers in preview

**Tasks:**
- [ ] Expand renderer coverage to 50+ components
- [ ] Add component interaction handlers
- [ ] Implement error highlighting in preview
- [ ] Add animation/transition support
- [ ] Create responsive preview (mobile/tablet/desktop)
- [ ] Add theme switching (light/dark)
- [ ] Implement screenshot/export functionality
- [ ] Add keyboard shortcuts
- [ ] Write comprehensive documentation

### Phase 6: Testing & Polish (Weeks 13-14)

**Goals:**
- Comprehensive testing across platforms
- Performance optimization
- Documentation

**Deliverables:**
1. 90%+ test coverage
2. Performance benchmarks <100ms render time
3. User documentation and tutorials

**Tasks:**
- [ ] Write integration tests for each platform
- [ ] Performance profiling and optimization
- [ ] Cross-browser testing (Web tool)
- [ ] Cross-IDE testing (VSCode, Android Studio)
- [ ] Create video tutorials
- [ ] Write API documentation
- [ ] Create migration guide from v0.1.0
- [ ] Prepare release notes

---

## Technical Specifications

### Performance Targets

| Metric | Target | Measurement |
|--------|--------|-------------|
| Parse Time | <50ms | Time from text change to AST ready |
| Render Time | <50ms | Time from AST update to screen paint |
| Total Latency | <150ms | End-to-end update latency |
| Memory Usage | <100MB | Peak memory for preview panel |
| Bundle Size (Web) | <500KB | Gzipped JavaScript bundle |
| File Size Limit | 10,000 lines | Maximum .ava file size |

### Browser Compatibility

**VSCode WebView:**
- Chromium 102+ (embedded)

**Android Studio:**
- JavaFX WebView (JDK 11+)

**Web Tool:**
- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

### API Specifications

#### WebView Message Protocol

**Extension → WebView:**
```typescript
interface ASTUpdateMessage {
    command: 'updateAST';
    ast: DocumentNode;
    errors: ParseError[];
}

interface PlatformChangeMessage {
    command: 'setPlatform';
    platform: 'android' | 'ios' | 'web' | 'desktop';
}
```

**WebView → Extension:**
```typescript
interface EventMessage {
    command: 'event';
    type: 'component-click' | 'component-hover';
    componentId: string;
}

interface ErrorMessage {
    command: 'error';
    error: string;
}
```

### Deployment Architecture

**VSCode Extension:**
- Distribution: VSIX package via VSCode Marketplace
- Update mechanism: VSCode auto-update
- Preview bundle: Included in VSIX

**Android Studio Plugin:**
- Distribution: JetBrains Marketplace
- Update mechanism: IntelliJ Platform update system
- Preview bundle: Packaged in plugin JAR

**Web Tool:**
- Distribution: Vercel/Netlify CDN
- Update mechanism: Automatic on git push
- CDN: Cloudflare (via Vercel)

---

## Security Considerations

### VSCode Extension

1. **Content Security Policy:**
   - Restrict script sources to extension bundle
   - No external CDN dependencies in WebView
   - Use nonce for inline scripts

2. **Code Execution:**
   - No `eval()` or `Function()` constructor
   - Sandboxed WebView context

### Android Studio Plugin

1. **WebView Security:**
   - Disable JavaScript execution from untrusted sources
   - Sanitize bridge communications
   - Validate all AST data before rendering

2. **File System Access:**
   - Read-only access to .ava files
   - No write permissions required

### Web Tool

1. **XSS Prevention:**
   - Sanitize all user input
   - Use React's built-in XSS protection
   - Content Security Policy headers

2. **Data Privacy:**
   - All parsing done client-side
   - No code sent to server
   - Optional localStorage (user consent)

---

## Appendix

### A. AST Node Definitions

```typescript
interface DocumentNode {
    type: 'Document';
    screens: ScreenNode[];
    range: Range;
}

interface ScreenNode {
    type: 'Screen';
    name: string;
    children: ComponentNode[];
    range: Range;
}

interface ComponentNode {
    type: 'Component';
    name: string;
    properties: PropertyNode[];
    children?: ComponentNode[];
    range: Range;
}

interface PropertyNode {
    type: 'Property';
    key: string;
    value: ValueNode;
    range: Range;
}

type ValueNode =
    | StringValueNode
    | NumberValueNode
    | BooleanValueNode
    | VariableValueNode
    | ComponentNode;

interface StringValueNode {
    type: 'StringValue';
    value: string;
    range: Range;
}

interface NumberValueNode {
    type: 'NumberValue';
    value: number;
    unit?: 'dp' | 'sp' | 'px' | 'pt';
    range: Range;
}

interface BooleanValueNode {
    type: 'BooleanValue';
    value: boolean;
    range: Range;
}

interface VariableValueNode {
    type: 'VariableValue';
    name: string;
    range: Range;
}
```

### B. Component Mapping Reference

| AVAMagic Component | Android (Compose) | iOS (SwiftUI) | Web (React) | Desktop (Flutter) |
|-------------------|-------------------|---------------|-------------|-------------------|
| AppBar | TopAppBar | NavigationBar | header | AppBar |
| Column | Column | VStack | div (flex-col) | Column |
| Row | Row | HStack | div (flex-row) | Row |
| Text | Text | Text | p/span | Text |
| Button | Button | Button | button | ElevatedButton |
| TextField | TextField | TextField | input | TextField |
| Image | Image | Image | img | Image |
| Card | Card | VStack + background | div.card | Card |
| Icon | Icon | Image(systemName) | svg/FontAwesome | Icon |
| Divider | Divider | Divider | hr | Divider |

### C. Color Scheme Mapping

```typescript
// Material Design colors for Android
const ANDROID_COLORS = {
    primary: '#6200EE',
    secondary: '#03DAC6',
    surface: '#FFFFFF',
    background: '#FAFAFA',
    error: '#B00020'
};

// iOS system colors
const IOS_COLORS = {
    primary: '#007AFF',
    secondary: '#5856D6',
    surface: '#FFFFFF',
    background: '#F2F2F7',
    error: '#FF3B30'
};

// Web default colors
const WEB_COLORS = {
    primary: '#1976D2',
    secondary: '#DC004E',
    surface: '#FFFFFF',
    background: '#F5F5F5',
    error: '#F44336'
};
```

---

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0.0 | 2025-11-27 | System | Initial comprehensive plan |

---

**End of Document**
