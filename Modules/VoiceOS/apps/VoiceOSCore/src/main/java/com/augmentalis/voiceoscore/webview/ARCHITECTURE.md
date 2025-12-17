# VOSWebView Architecture

## Component Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                          Voice OS Core                              │
│                                                                     │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │                      VOSWebView                            │   │
│  │                  (Main WebView Class)                      │   │
│  │                                                            │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │   │
│  │  │   Settings   │  │  Listeners   │  │   Commands   │   │   │
│  │  ├──────────────┤  ├──────────────┤  ├──────────────┤   │   │
│  │  │ JavaScript   │  │ WebViewClient│  │ clickElement │   │   │
│  │  │ DOMStorage   │  │ ChromeClient │  │ focusElement │   │   │
│  │  │ NoFileAccess │  │ CommandLstnr │  │ scrollTo     │   │   │
│  │  └──────────────┘  └──────────────┘  │ fillInput    │   │   │
│  │                                       └──────────────┘   │   │
│  └────────────┬───────────────────────────┬──────────────────┘   │
│               │                           │                       │
│      ┌────────▼────────┐         ┌───────▼────────┐             │
│      │ VOSWebInterface │         │WebCommandExec  │             │
│      │  (JS Bridge)    │         │ (JS Generator) │             │
│      └────────┬────────┘         └───────┬────────┘             │
│               │                           │                       │
└───────────────┼───────────────────────────┼───────────────────────┘
                │                           │
        ┌───────▼───────────────────────────▼───────┐
        │         JavaScript Context                │
        │         (window.VOS)                      │
        │                                           │
        │  window.VOS.executeCommand()             │
        │  window.VOS.clickElement()               │
        │  window.VOS.focusElement()               │
        │  window.VOS.scrollToElement()            │
        │  window.VOS.fillInput()                  │
        │  window.VOS.getAvailableCommands()       │
        │  window.VOS.logEvent()                   │
        │                                           │
        │  window.VOSCommands (helper)             │
        │    - click(), focus(), scroll(), fill()  │
        │    - execute(), getAvailable()           │
        └───────────────────────────────────────────┘
                           │
                           │
                  ┌────────▼────────┐
                  │   Web Page DOM  │
                  │                 │
                  │  ┌───────────┐  │
                  │  │ Elements  │  │
                  │  ├───────────┤  │
                  │  │ Buttons   │  │
                  │  │ Inputs    │  │
                  │  │ Links     │  │
                  │  │ Forms     │  │
                  │  └───────────┘  │
                  └─────────────────┘
```

## Class Relationships

```
┌─────────────────────────────────────────────────────────────────┐
│                         VOSWebView                              │
│                       (extends WebView)                         │
├─────────────────────────────────────────────────────────────────┤
│ - commandExecutor: WebCommandExecutor                           │
│ - commandListener: CommandListener?                             │
│                                                                 │
│ + setCommandListener(listener)                                  │
│ + executeCommand(command, xpath, value)                         │
│ + clickElement(xpath)                                           │
│ + focusElement(xpath)                                           │
│ + scrollToElement(xpath)                                        │
│ + fillInput(xpath, value)                                       │
│ + getAvailableCommands()                                        │
├─────────────────────────────────────────────────────────────────┤
│ Inner Classes:                                                  │
│ - VOSWebViewClient (extends WebViewClient)                      │
│ - VOSWebChromeClient (extends WebChromeClient)                  │
│                                                                 │
│ Interfaces:                                                     │
│ - CommandListener                                               │
│   + onCommandExecuted(command, success)                         │
│   + onCommandError(command, error)                              │
│   + onCommandsDiscovered(commands)                              │
└─────────────────────────────────────────────────────────────────┘
                           │
                           │ uses
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    VOSWebInterface                              │
│              (JavaScript Interface Bridge)                      │
├─────────────────────────────────────────────────────────────────┤
│ - webView: VOSWebView                                           │
│ - executor: WebCommandExecutor                                  │
│ - listener: CommandListener?                                    │
│                                                                 │
│ @JavascriptInterface                                            │
│ + executeCommand(command, xpath, value): Boolean                │
│ + getAvailableCommands(): String                                │
│ + clickElement(xpath): Boolean                                  │
│ + focusElement(xpath): Boolean                                  │
│ + scrollToElement(xpath): Boolean                               │
│ + fillInput(xpath, value): Boolean                              │
│ + logEvent(message)                                             │
│ + reportDiscoveredCommands(commandsJson)                        │
│                                                                 │
│ - sanitizeXPath(xpath): String                                  │
│ - isJavaScriptSafe(value): Boolean                              │
│ - escapeForJavaScript(value): String                            │
└─────────────────────────────────────────────────────────────────┘
                           │
                           │ uses
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                   WebCommandExecutor                            │
│              (JavaScript Code Generator)                        │
├─────────────────────────────────────────────────────────────────┤
│ - context: Context                                              │
│ - availableCommands: List<String>                               │
│                                                                 │
│ + executeCommand(webView, command, xpath, value): Boolean       │
│ + getAvailableCommands(): String                                │
│ + updateAvailableCommands(commands)                             │
│                                                                 │
│ - generateClickJS(xpath): String                                │
│ - generateFocusJS(xpath): String                                │
│ - generateScrollJS(xpath): String                               │
│ - generateFillJS(xpath, value): String                          │
│ - generateSubmitJS(xpath): String                               │
│ - generateSelectJS(xpath, value): String                        │
│ - generateCheckJS(xpath, value): String                         │
│ - generateRadioJS(xpath): String                                │
└─────────────────────────────────────────────────────────────────┘
```

## Sequence Diagram: Command Execution

```
User Voice   VOSWebView   VOSWebInterface   WebCommandExecutor   JavaScript   DOM
  │              │              │                  │                │          │
  │──"click"────>│              │                  │                │          │
  │              │              │                  │                │          │
  │              │─clickElement()─>                │                │          │
  │              │              │                  │                │          │
  │              │              │─executeCommand()─>                │          │
  │              │              │                  │                │          │
  │              │              │                  │─generateJS()──>│          │
  │              │              │                  │                │          │
  │              │<─────────────────evaluateJavascript()───────────│          │
  │              │              │                  │                │          │
  │              │──────────────────────────────────────────────────>          │
  │              │              │                  │       XPath Eval          │
  │              │              │                  │                │<─────────│
  │              │              │                  │                │          │
  │              │              │                  │                │──click()─>│
  │              │              │                  │                │          │
  │              │              │                  │                │<─success─│
  │              │              │                  │                │          │
  │              │<─────────────────────────────result(true)────────│          │
  │              │              │                  │                │          │
  │              │─onCommandExecuted()────>        │                │          │
  │              │              │                  │                │          │
  │<─success────│              │                  │                │          │
  │              │              │                  │                │          │
```

## Sequence Diagram: Page Load & Command Discovery

```
Browser   VOSWebView   VOSWebViewClient   JavaScript   VOSWebInterface   CommandListener
  │           │              │                │                │                │
  │─loadUrl()─>              │                │                │                │
  │           │              │                │                │                │
  │           │────────────load page─────────>│                │                │
  │           │              │                │                │                │
  │           │<─onPageFinished()─────────────│                │                │
  │           │              │                │                │                │
  │           │─injectJS()───>                │                │                │
  │           │              │                │                │                │
  │           │──────────────────────────────>│                │                │
  │           │         evaluateJavascript()  │                │                │
  │           │              │                │                │                │
  │           │              │                │─discover()────>│                │
  │           │              │                │   elements     │                │
  │           │              │                │                │                │
  │           │              │                │<─buttons,─────>│                │
  │           │              │                │  links, etc    │                │
  │           │              │                │                │                │
  │           │              │                │─report()───────>                │
  │           │              │                │                │                │
  │           │              │                │<─JSON─────────>│                │
  │           │              │                │                │                │
  │           │<─────────────────────────onCommandsDiscovered()────────────────│
  │           │              │                │                │                │
  │<─commands─│              │                │                │                │
  │           │              │                │                │                │
```

## Data Flow: Voice Command to DOM Interaction

```
┌──────────────────┐
│  Voice Input     │
│  "click submit"  │
└────────┬─────────┘
         │
         ▼
┌──────────────────────────────┐
│  Voice Recognition           │
│  (External Component)        │
└────────┬─────────────────────┘
         │
         │ voice command string
         ▼
┌──────────────────────────────┐
│  Command Parser              │
│  - Extract action (click)    │
│  - Extract target (submit)   │
│  - Map to XPath              │
└────────┬─────────────────────┘
         │
         │ (command="CLICK", xpath="//button[@id='submit']")
         ▼
┌──────────────────────────────┐
│  VOSWebView                  │
│  clickElement(xpath)         │
└────────┬─────────────────────┘
         │
         │ validate & sanitize
         ▼
┌──────────────────────────────┐
│  VOSWebInterface             │
│  executeCommand()            │
└────────┬─────────────────────┘
         │
         │ generate JavaScript
         ▼
┌──────────────────────────────┐
│  WebCommandExecutor          │
│  generateClickJS()           │
└────────┬─────────────────────┘
         │
         │ JavaScript code
         ▼
┌──────────────────────────────┐
│  evaluateJavascript()        │
│  Execute in WebView context  │
└────────┬─────────────────────┘
         │
         │ XPath evaluation
         ▼
┌──────────────────────────────┐
│  DOM Traversal               │
│  Find element by XPath       │
└────────┬─────────────────────┘
         │
         │ element found
         ▼
┌──────────────────────────────┐
│  DOM Interaction             │
│  element.click()             │
└────────┬─────────────────────┘
         │
         │ success/failure
         ▼
┌──────────────────────────────┐
│  Callback Chain              │
│  onCommandExecuted()         │
└──────────────────────────────┘
```

## Security Architecture

```
┌───────────────────────────────────────────────────────────────┐
│                     Security Layers                           │
└───────────────────────────────────────────────────────────────┘

Layer 1: Input Validation
┌──────────────────────────────────────────────────────────────┐
│  VOSWebInterface                                             │
│  ✓ Command type whitelist                                   │
│  ✓ XPath non-empty check                                    │
│  ✓ Value parameter validation                               │
└──────────────────────────────────────────────────────────────┘
                          ↓
Layer 2: XPath Sanitization
┌──────────────────────────────────────────────────────────────┐
│  sanitizeXPath()                                             │
│  ✓ Remove <script> tags                                     │
│  ✓ Remove event handlers (onclick, onload, etc.)            │
│  ✓ Pattern matching for dangerous content                   │
└──────────────────────────────────────────────────────────────┘
                          ↓
Layer 3: JavaScript Escaping
┌──────────────────────────────────────────────────────────────┐
│  escapeForJavaScript()                                       │
│  ✓ Escape backslashes                                       │
│  ✓ Escape quotes                                            │
│  ✓ Escape control characters                                │
└──────────────────────────────────────────────────────────────┘
                          ↓
Layer 4: WebView Sandboxing
┌──────────────────────────────────────────────────────────────┐
│  WebView Settings                                            │
│  ✓ allowFileAccess = false                                  │
│  ✓ allowContentAccess = false                               │
│  ✓ safeBrowsingEnabled = true                               │
│  ✓ mixedContentMode = NEVER_ALLOW                           │
└──────────────────────────────────────────────────────────────┘
                          ↓
Layer 5: JavaScript Interface
┌──────────────────────────────────────────────────────────────┐
│  @JavascriptInterface                                        │
│  ✓ Explicit method annotation (API 17+)                     │
│  ✓ Minimal exposed surface                                  │
│  ✓ No arbitrary code execution                              │
└──────────────────────────────────────────────────────────────┘
```

## Error Handling Flow

```
┌────────────────────────────────────────────────────────────────┐
│                    Error Detection Points                      │
└────────────────────────────────────────────────────────────────┘

Input Validation Error
      │
      ├─> VOSWebInterface
      │   └─> onCommandError("INVALID_INPUT", error)
      │
XPath Evaluation Error
      │
      ├─> JavaScript: element not found
      │   └─> return false
      │       └─> onCommandExecuted(command, success=false)
      │
JavaScript Execution Error
      │
      ├─> evaluateJavascript() catches exception
      │   └─> onCommandError("JS_ERROR", exception.message)
      │
Page Load Error
      │
      └─> VOSWebViewClient.onReceivedError()
          └─> onCommandError("PAGE_LOAD", error)

┌────────────────────────────────────────────────────────────────┐
│                    Error Recovery Strategy                     │
└────────────────────────────────────────────────────────────────┘

Error Type              Recovery Action
───────────────────────────────────────────────────────────────
Element Not Found  ──>  Try alternative XPath
Timeout           ──>  Retry after delay
Invalid XPath     ──>  Suggest correction
Page Load Failed  ──>  Reload or navigate back
JavaScript Error  ──>  Log and report to user
Security Violation ──> Block and log attempt
```

## Integration Architecture

```
┌───────────────────────────────────────────────────────────────┐
│                      Voice OS Core                            │
│                                                               │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐     │
│  │   Voice     │    │   Learn     │    │   Command   │     │
│  │ Recognition │    │    Web      │    │   Manager   │     │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘     │
│         │                  │                   │             │
│         │ voice            │ commands          │ execute     │
│         │ commands         │ discovered        │ command     │
│         └─────────┐  ┌─────┘                   │             │
│                   │  │                         │             │
│           ┌───────▼──▼─────────────────────────▼───────┐    │
│           │          VOSWebView                         │    │
│           │                                             │    │
│           │  • Execute voice commands                   │    │
│           │  • Discover page commands                   │    │
│           │  • Report results                           │    │
│           └─────────────────┬───────────────────────────┘    │
│                             │                                 │
└─────────────────────────────┼─────────────────────────────────┘
                              │
                              │ JavaScript bridge
                              │
                    ┌─────────▼──────────┐
                    │    Web Page        │
                    │    (Any Website)   │
                    └────────────────────┘
```

## Deployment Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                    Android Application                         │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  Activity/Fragment                                             │
│  ├─ VoiceRecognition Service                                   │
│  ├─ VOSWebView                                                 │
│  │  ├─ VOSWebInterface (window.VOS)                            │
│  │  ├─ WebCommandExecutor                                      │
│  │  ├─ VOSWebViewClient                                        │
│  │  └─ VOSWebChromeClient                                      │
│  └─ CommandListener Implementation                             │
│                                                                │
├────────────────────────────────────────────────────────────────┤
│                    Android System                              │
├────────────────────────────────────────────────────────────────┤
│  WebView Component (Chromium)                                  │
│  ├─ JavaScript Engine (V8)                                     │
│  ├─ DOM Parser                                                 │
│  ├─ XPath Evaluator                                            │
│  └─ Security Sandbox                                           │
└────────────────────────────────────────────────────────────────┘
```

---

**Generated:** 2025-10-13 05:12:27 PDT
**Version:** 1.0.0
**Status:** Complete
