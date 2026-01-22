# CodeAvenue Terminal App V2 - Addendum: Docs & Browser
ver: V1 | date: 260122 | status: draft

## Overview
Addendum to V2 design adding:
1. **Mintlify-style Documentation** - Auto-generated, AI-maintained docs
2. **Built-in AI Browser** - WebView with DOM access for LLM agents

---

## Feature 1: AI Documentation System

### Mintlify-Inspired Capabilities

| Feature | Description |
|---------|-------------|
| **Auto-generation** | Generate docs from code, comments, types |
| **Self-updating** | AI keeps docs in sync with code changes |
| **Context-aware agent** | Draft, edit, maintain content |
| **LLMs.txt support** | Make docs available to AI tools |
| **API Reference** | Auto-build from code/OpenAPI |
| **Multi-format export** | Markdown, HTML, JSON |

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Documentation Engine                         │
├─────────────────────────────────────────────────────────────────┤
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌──────────┐ │
│  │ Code Parser│  │ Doc Parser │  │ AI Agent   │  │ Renderer │ │
│  │ (tree-sit) │  │ (markdown) │  │ (LLM)      │  │ (MDX)    │ │
│  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘  └────┬─────┘ │
│        │               │               │               │        │
│        └───────────────┴───────────────┴───────────────┘        │
│                              │                                   │
│                    ┌─────────▼─────────┐                        │
│                    │   Doc Database    │                        │
│                    │   (SQLite)        │                        │
│                    └───────────────────┘                        │
└─────────────────────────────────────────────────────────────────┘
```

### Documentation Types

```typescript
interface DocNode {
  id: string;
  type: DocType;
  path: string;
  title: string;
  content: string;
  metadata: DocMetadata;
  children: string[]; // Child doc IDs

  // Auto-generation tracking
  sourceFile?: string;
  sourceLine?: number;
  lastSyncedAt?: number;
  isStale: boolean;
}

type DocType =
  | 'module'      // Module overview
  | 'class'       // Class documentation
  | 'function'    // Function documentation
  | 'api'         // API endpoint
  | 'guide'       // User guide
  | 'tutorial'    // Step-by-step
  | 'reference'   // Reference material
  | 'changelog'   // Version history
  | 'readme';     // Project README

interface DocMetadata {
  author?: string;
  version?: string;
  tags: string[];
  related: string[];
  deprecated?: boolean;
  since?: string;
}
```

### Code-to-Doc Extraction

```typescript
interface CodeExtractor {
  // Parse source file
  parse(filePath: string): Promise<CodeSymbols>;

  // Extract documentation
  extractDocs(symbols: CodeSymbols): Promise<DocNode[]>;

  // Detect changes
  detectChanges(file: string): Promise<CodeChange[]>;
}

interface CodeSymbols {
  classes: ClassSymbol[];
  functions: FunctionSymbol[];
  interfaces: InterfaceSymbol[];
  types: TypeSymbol[];
  exports: ExportSymbol[];
}

interface ClassSymbol {
  name: string;
  filePath: string;
  line: number;
  docComment?: string;
  methods: MethodSymbol[];
  properties: PropertySymbol[];
  decorators: string[];
  extends?: string;
  implements: string[];
}
```

### AI Documentation Agent

```typescript
interface DocAgent {
  // Generate documentation from code
  generateFromCode(symbols: CodeSymbols): Promise<DocNode[]>;

  // Update documentation when code changes
  syncWithCode(changes: CodeChange[]): Promise<DocUpdate[]>;

  // Improve existing documentation
  improve(docId: string, feedback?: string): Promise<DocNode>;

  // Answer questions about codebase
  query(question: string): Promise<DocResponse>;

  // Generate API reference
  generateApiRef(openApiSpec: OpenAPISpec): Promise<DocNode[]>;
}

interface DocUpdate {
  docId: string;
  action: 'create' | 'update' | 'delete' | 'archive';
  diff?: string;
  reason: string;
  confidence: number;
}
```

### Documentation UI

```
┌─────────────────────────────────────────────────────────────────┐
│ 📚 Documentation                                    [⟳] [⚙️]   │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┬───────────────────────────────────────┐   │
│  │ 📁 Modules      │ # UserService                         │   │
│  │  ├─ Auth        │                                       │   │
│  │  │  ├─ Login    │ Authentication service for users.     │   │
│  │  │  └─ Signup   │                                       │   │
│  │  ├─ API         │ ## Methods                            │   │
│  │  │  ├─ Users    │                                       │   │
│  │  │  └─ Orders   │ ### `authenticate(email, password)`   │   │
│  │  └─ Utils       │ Authenticates user with credentials.  │   │
│  │                 │                                       │   │
│  │ 🔄 Stale: 3     │ **Parameters:**                       │   │
│  │ ⚠️ Missing: 2   │ - `email` - User email                │   │
│  │                 │ - `password` - User password          │   │
│  │ [+ New Doc]     │                                       │   │
│  │ [🤖 Generate]   │ **Returns:** `Promise<AuthResult>`    │   │
│  └─────────────────┴───────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────┤
│ [Edit] [Regenerate] [View Source] [Export]                      │
└─────────────────────────────────────────────────────────────────┘
```

### LLMs.txt Generation

```typescript
interface LLMsTextGenerator {
  // Generate llms.txt for AI tools
  generate(project: ProjectConfig): Promise<string>;
}

// Example output:
// # CodeAvenue
// > AI-powered development terminal with multi-agent support
//
// ## Modules
// - **auth**: User authentication (login, signup, OAuth)
// - **api**: REST API endpoints
// - **terminal**: PTY management and terminal UI
//
// ## API Reference
// GET /health - Health check
// POST /auth/login - User login
// ...
```

### Commands

| Command | Action |
|---------|--------|
| `/docs` | Open documentation panel |
| `/docs .generate` | Generate docs for current file |
| `/docs .sync` | Sync all stale documentation |
| `/docs .export` | Export docs to markdown/HTML |
| `/docs .search <query>` | Search documentation |
| `/docs .llms` | Generate llms.txt |

---

## Feature 2: AI Browser (WebView)

### Purpose

Give AI/LLM agents direct browser access for:
- Web research during coding tasks
- Testing web applications
- Scraping documentation sites
- Interacting with web APIs
- Viewing deployed applications

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     AI Browser Module                           │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    WebView (wry/tao)                     │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │              Rendered Web Page                   │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  └───────────────────────────┬─────────────────────────────┘   │
│                              │                                  │
│  ┌───────────────────────────▼─────────────────────────────┐   │
│  │                  JavaScript Bridge                       │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐ │   │
│  │  │ DOM      │  │ Click    │  │ Input    │  │ Screen- │ │   │
│  │  │ Scraper  │  │ Handler  │  │ Handler  │  │ shot    │ │   │
│  │  └──────────┘  └──────────┘  └──────────┘  └─────────┘ │   │
│  └───────────────────────────┬─────────────────────────────┘   │
│                              │                                  │
│  ┌───────────────────────────▼─────────────────────────────┐   │
│  │                   AI Browser API                         │   │
│  │  navigate() | click() | type() | screenshot() | scrape()│   │
│  └───────────────────────────┬─────────────────────────────┘   │
│                              │                                  │
│  ┌───────────────────────────▼─────────────────────────────┐   │
│  │                Terminal/Agent Interface                  │   │
│  │  AI can request browser actions, receive DOM state       │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### Browser Capabilities (from WebAvanue)

Leveraging your existing `DOMScraperBridge`:

```typescript
interface AIBrowser {
  // Navigation
  navigate(url: string): Promise<NavigationResult>;
  back(): Promise<void>;
  forward(): Promise<void>;
  refresh(): Promise<void>;

  // DOM Access (via DOMScraperBridge)
  scrapeDOM(): Promise<DOMSnapshot>;
  getElements(selector: string): Promise<DOMElement[]>;

  // Interaction
  click(selector: string): Promise<ActionResult>;
  type(selector: string, text: string): Promise<ActionResult>;
  select(selector: string, value: string): Promise<ActionResult>;
  scroll(direction: 'up' | 'down' | 'to', target?: string): Promise<void>;
  hover(selector: string): Promise<void>;

  // Capture
  screenshot(): Promise<ImageData>;
  screenshotElement(selector: string): Promise<ImageData>;

  // Content
  getPageContent(): Promise<string>;
  extractText(selector: string): Promise<string>;
  extractLinks(): Promise<Link[]>;

  // Tabs
  newTab(): Promise<string>;
  closeTab(tabId: string): Promise<void>;
  switchTab(tabId: string): Promise<void>;
  listTabs(): Promise<Tab[]>;

  // Wait
  waitForElement(selector: string, timeout?: number): Promise<boolean>;
  waitForNavigation(timeout?: number): Promise<void>;
}

interface DOMSnapshot {
  url: string;
  title: string;
  timestamp: number;
  viewport: Viewport;
  elements: DOMElement[];
  elementCount: number;
}

interface DOMElement {
  id: string;
  tag: string;
  type: string;      // 'link' | 'button' | 'input' | 'dropdown' | etc.
  name: string;      // Accessible name
  role: string;      // ARIA role
  selector: string;  // CSS selector
  xpath: string;     // XPath
  bounds: Rect;
  isDisabled: boolean;
  isChecked: boolean;
  value: string;
  href: string;
}
```

### Rust Backend (Tauri WebView)

```rust
// src-tauri/src/browser/mod.rs

use wry::WebView;
use serde::{Deserialize, Serialize};

pub struct AIBrowser {
    webview: Option<WebView>,
    current_url: String,
    dom_cache: Option<DOMSnapshot>,
}

impl AIBrowser {
    pub fn new() -> Self {
        Self {
            webview: None,
            current_url: String::new(),
            dom_cache: None,
        }
    }

    /// Navigate to URL
    pub async fn navigate(&mut self, url: &str) -> Result<NavigationResult, BrowserError> {
        if let Some(wv) = &self.webview {
            wv.evaluate_script(&format!("window.location.href = '{}';", url))?;
            self.current_url = url.to_string();
            self.wait_for_load().await?;
            Ok(NavigationResult::success(url))
        } else {
            Err(BrowserError::NotInitialized)
        }
    }

    /// Scrape DOM using DOMScraperBridge
    pub async fn scrape_dom(&mut self) -> Result<DOMSnapshot, BrowserError> {
        if let Some(wv) = &self.webview {
            let script = include_str!("../scripts/dom_scraper.js");
            let result = wv.evaluate_script(script)?;
            let snapshot: DOMSnapshot = serde_json::from_str(&result)?;
            self.dom_cache = Some(snapshot.clone());
            Ok(snapshot)
        } else {
            Err(BrowserError::NotInitialized)
        }
    }

    /// Click element
    pub async fn click(&self, selector: &str) -> Result<ActionResult, BrowserError> {
        if let Some(wv) = &self.webview {
            let script = format!(
                r#"(function() {{
                    const el = document.querySelector('{}');
                    if (el) {{
                        el.click();
                        return JSON.stringify({{ success: true }});
                    }}
                    return JSON.stringify({{ success: false, error: 'Not found' }});
                }})();"#,
                selector.replace("'", "\\'")
            );
            let result = wv.evaluate_script(&script)?;
            Ok(serde_json::from_str(&result)?)
        } else {
            Err(BrowserError::NotInitialized)
        }
    }

    /// Take screenshot
    pub async fn screenshot(&self) -> Result<Vec<u8>, BrowserError> {
        // Use platform-specific screenshot capture
        // On macOS: CGWindowListCreateImage
        // On Windows: BitBlt
        // On Linux: XGetImage
        unimplemented!()
    }
}

// Tauri Commands
#[tauri::command]
pub async fn browser_navigate(url: String) -> Result<NavigationResult, String>;

#[tauri::command]
pub async fn browser_scrape() -> Result<DOMSnapshot, String>;

#[tauri::command]
pub async fn browser_click(selector: String) -> Result<ActionResult, String>;

#[tauri::command]
pub async fn browser_type(selector: String, text: String) -> Result<ActionResult, String>;

#[tauri::command]
pub async fn browser_screenshot() -> Result<Vec<u8>, String>;
```

### Browser UI Modes

#### Mode 1: Side Panel
```
┌─────────────────────────────────────────────────────────────────┐
│ Terminals (70%)                      │ Browser (30%)            │
│ ┌─────────┐ ┌─────────┐              │ ┌─────────────────────┐  │
│ │ T1      │ │ T2      │              │ │ 🌐 google.com       │  │
│ │         │ │         │              │ ├─────────────────────┤  │
│ │         │ │         │              │ │                     │  │
│ │         │ │         │              │ │    [Web Page]       │  │
│ │         │ │         │              │ │                     │  │
│ └─────────┘ └─────────┘              │ └─────────────────────┘  │
└──────────────────────────────────────┴──────────────────────────┘
```

#### Mode 2: Tab Panel
```
┌─────────────────────────────────────────────────────────────────┐
│ [Terminals] [Browser] [Docs] [PR]                               │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 🌐 ← → ⟳ | https://docs.anthropic.com                   │   │
│  ├─────────────────────────────────────────────────────────┤   │
│  │                                                          │   │
│  │                    [Web Page Content]                    │   │
│  │                                                          │   │
│  │                                                          │   │
│  └─────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────┤
│ AI can: [Scrape] [Click element] [Fill form] [Screenshot]       │
└─────────────────────────────────────────────────────────────────┘
```

#### Mode 3: Floating Window
```
┌─────────────────────────────────────────────────────────────────┐
│ Main Terminal View                                              │
│                                                                 │
│   ┌─────────────────────────┐                                   │
│   │ 🌐 Floating Browser     │                                   │
│   │ [drag handle]       [x] │                                   │
│   ├─────────────────────────┤                                   │
│   │                         │                                   │
│   │   [Web Page]            │                                   │
│   │                         │                                   │
│   └─────────────────────────┘                                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### AI Agent Browser Commands

```typescript
// Natural language to browser action
interface BrowserCommand {
  // "Go to anthropic.com"
  navigate: { url: string };

  // "Click the login button"
  click: { description: string };

  // "Type my email in the email field"
  type: { target: string; text: string };

  // "Take a screenshot"
  screenshot: {};

  // "What's on this page?"
  describe: {};

  // "Find all links on this page"
  extract: { type: 'links' | 'text' | 'forms' | 'images' };

  // "Scroll down"
  scroll: { direction: 'up' | 'down'; amount?: number };
}
```

### AI Browser Workflow Example

```
User: "Research React Server Components and summarize"

AI Agent:
1. browser_navigate("https://react.dev/reference/rsc/server-components")
2. browser_scrape() -> Get DOM elements
3. browser_extract_text("article") -> Get article content
4. browser_screenshot() -> Capture for reference
5. Summarize content for user

User: "Test the login form on localhost:3000"

AI Agent:
1. browser_navigate("http://localhost:3000/login")
2. browser_scrape() -> Find form elements
3. browser_type("#email", "test@example.com")
4. browser_type("#password", "test123")
5. browser_click("button[type='submit']")
6. browser_wait_for_navigation()
7. browser_screenshot() -> Capture result
8. Report: "Login successful, redirected to dashboard"
```

### Security Considerations

```typescript
interface BrowserSecurity {
  // Allowed domains (whitelist)
  allowedDomains: string[];

  // Blocked domains (blacklist)
  blockedDomains: string[];

  // Require approval for
  requireApproval: {
    formSubmit: boolean;
    fileDownload: boolean;
    externalNavigation: boolean;
    sensitiveInput: boolean;  // passwords, credit cards
  };

  // Sandboxing
  sandbox: {
    disableJavaScript: boolean;
    disablePlugins: boolean;
    blockPopups: boolean;
    blockCookies: boolean;
  };

  // Private mode
  incognito: boolean;
}
```

### Commands

| Command | Action |
|---------|--------|
| `/browser` | Open/toggle browser panel |
| `/browser <url>` | Navigate to URL |
| `/browse <query>` | Search and navigate |
| `/screenshot` | Capture current page |
| `/scrape` | Get DOM elements |
| `@url:<url>` | Include URL content in context |

---

## Implementation Priority

### Phase 1: Browser Core (Week 1)
| Task | Effort |
|------|--------|
| Tauri WebView integration | 2d |
| DOM scraper integration | 1d |
| Basic navigation/click/type | 2d |
| Screenshot capture | 1d |

### Phase 2: Browser UI (Week 2)
| Task | Effort |
|------|--------|
| Browser panel component | 2d |
| Tab management | 1d |
| Address bar | 1d |
| View mode switching | 1d |

### Phase 3: Documentation Core (Week 3)
| Task | Effort |
|------|--------|
| tree-sitter code parsing | 2d |
| Doc database schema | 1d |
| AI doc generation | 2d |
| Markdown rendering | 1d |

### Phase 4: Documentation UI (Week 4)
| Task | Effort |
|------|--------|
| Doc tree component | 2d |
| Doc editor | 2d |
| Sync status indicators | 1d |

---

## Updated V2 Timeline

| Phase | Duration | Features |
|-------|----------|----------|
| 1 | 2 weeks | Core infrastructure, file explorer |
| 2 | 2 weeks | Diff view, approval workflow |
| 3 | 2 weeks | GitHub/GitLab integration |
| 4 | 1 week | Context management, @mentions |
| 5 | 1 week | Task history, restore |
| **6** | **2 weeks** | **AI Browser** |
| **7** | **2 weeks** | **Documentation System** |
| 8 | 1 week | Polish, keyboard shortcuts |

**Total: ~13 weeks** (was 9 weeks, +4 for browser & docs)

---
Author: Manoj Jhawar | v1 | 260122
