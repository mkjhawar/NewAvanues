# Chapter 27: Web Scraping Tool (JavaScript)

**Status:** Complete Guide
**Last Updated:** 2025-11-03
**Scope:** Browser extension architecture, DOM scraping, dynamic content, cross-browser support

---

## Table of Contents

1. [Overview](#overview)
2. [Browser Extension Architecture](#browser-extension-architecture)
3. [DOM Scraping Engine](#dom-scraping-engine)
4. [Dynamic Content Handling](#dynamic-content-handling)
5. [Cross-Browser Support](#cross-browser-support)
6. [Native App Integration](#native-app-integration)
7. [Security & CSP](#security--csp)
8. [JavaScript Examples](#javascript-examples)
9. [Testing & Debugging](#testing--debugging)
10. [Deployment Guide](#deployment-guide)

---

## Overview

The VOS4 Web Scraping Tool is a browser extension that provides web content extraction and manipulation capabilities for voice-driven interactions. Unlike native UI scraping (which works with platform accessibility APIs), web scraping operates on the DOM (Document Object Model) of web pages and web applications.

**Key Capabilities:**
- Extract page structure, text, and metadata
- Identify clickable elements and input fields
- Detect dynamic content changes (SPAs, real-time updates)
- Execute actions on page elements
- Integrate with native apps via messaging API
- Support voice command targeting

**Supported Browsers:**
- Chrome/Chromium (Manifest V3)
- Firefox (Manifest V2/V3)
- Safari (Web Extensions)
- Edge (Chromium-based)

**Architecture:**
- **Manifest:** Extension configuration
- **Background Script:** Extension lifecycle, messaging
- **Content Script:** DOM access and manipulation
- **Popup/Options:** UI for extension configuration
- **Native Messaging:** Communication with native apps

---

## Browser Extension Architecture

### Manifest V3 Configuration

The extension is defined by its manifest, which declares permissions, scripts, and capabilities:

```json
{
  "manifest_version": 3,
  "name": "VOS4 Web Assistant",
  "version": "1.0.0",
  "description": "Voice-driven web interaction for VOS4",

  "permissions": [
    "scripting",
    "activeTab",
    "tabs",
    "storage",
    "webRequest"
  ],

  "host_permissions": [
    "<all_urls>"
  ],

  "background": {
    "service_worker": "background.js",
    "type": "module"
  },

  "content_scripts": [
    {
      "matches": ["<all_urls>"],
      "js": ["content.js"],
      "run_at": "document_start",
      "all_frames": true
    }
  ],

  "web_accessible_resources": [
    {
      "resources": ["inject.js"],
      "matches": ["<all_urls>"]
    }
  ],

  "action": {
    "default_popup": "popup.html",
    "default_title": "VOS4 Web Assistant",
    "default_icon": "icons/icon-48.png"
  },

  "icons": {
    "16": "icons/icon-16.png",
    "48": "icons/icon-48.png",
    "128": "icons/icon-128.png"
  },

  "native_messaging": true,
  "permissions": ["nativeMessaging"],

  "externally_connectable": {
    "matches": ["*://localhost/*"]
  }
}
```

### Background Script (Service Worker)

The background script manages extension lifecycle and message routing:

```javascript
// background.js
const NATIVE_APP_ID = 'com.augmentalis.vos4.webassistant';

// Port management for content scripts
const contentPorts = new Map();

// Listen for content scripts to connect
chrome.runtime.onConnect.addListener((port) => {
  if (port.name === 'content-script') {
    const tabId = port.sender.tab.id;
    contentPorts.set(tabId, port);

    port.onDisconnect.addListener(() => {
      contentPorts.delete(tabId);
    });

    port.onMessage.addListener((request, sendResponse) => {
      handleContentScriptMessage(request, port);
    });
  }
});

// Listen for messages from popup
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (request.action === 'extract') {
    handleExtractRequest(request, sender, sendResponse);
  } else if (request.action === 'execute') {
    handleExecuteRequest(request, sender, sendResponse);
  } else if (request.action === 'toggle-monitoring') {
    handleToggleMonitoring(request, sender, sendResponse);
  }
});

// Listen for native app messages
chrome.runtime.connectNative(NATIVE_APP_ID);

chrome.runtime.onNativeMessage.addListener((request, sender, sendResponse) => {
  // Forward native app requests to content script
  const tabId = request.tabId;
  const port = contentPorts.get(tabId);

  if (port) {
    port.postMessage({
      source: 'native-app',
      action: request.action,
      data: request.data
    });
  }

  sendResponse({ received: true });
});

function handleContentScriptMessage(request, port) {
  const tabId = port.sender.tab.id;

  if (request.source === 'content-script') {
    if (request.action === 'page-snapshot') {
      // Forward page snapshot to native app
      sendToNativeApp({
        action: 'page-snapshot',
        tabId: tabId,
        data: request.data
      });
    } else if (request.action === 'element-changed') {
      // Notify about DOM changes
      broadcastToPopups({
        action: 'element-changed',
        tabId: tabId,
        elementId: request.elementId,
        changes: request.changes
      });
    }
  }
}

function handleExtractRequest(request, sender, sendResponse) {
  const tabId = sender.tab.id;
  const port = contentPorts.get(tabId);

  if (port) {
    port.postMessage({
      source: 'background',
      action: 'extract-elements',
      selector: request.selector
    }, (response) => {
      sendResponse(response);
    });
  } else {
    sendResponse({ error: 'No content script active' });
  }
}

function handleExecuteRequest(request, sender, sendResponse) {
  const tabId = sender.tab.id;
  const port = contentPorts.get(tabId);

  if (port) {
    port.postMessage({
      source: 'background',
      action: 'execute-action',
      elementId: request.elementId,
      action: request.action
    }, (response) => {
      sendResponse(response);
    });
  } else {
    sendResponse({ error: 'No content script active' });
  }
}

function handleToggleMonitoring(request, sender, sendResponse) {
  const tabId = sender.tab.id;
  const port = contentPorts.get(tabId);

  if (port) {
    port.postMessage({
      source: 'background',
      action: 'toggle-monitoring',
      enabled: request.enabled
    });
  }

  sendResponse({ received: true });
}

function sendToNativeApp(message) {
  chrome.runtime.sendNativeMessage(NATIVE_APP_ID, message, (response) => {
    if (chrome.runtime.lastError) {
      console.error('Native app error:', chrome.runtime.lastError);
    }
  });
}

function broadcastToPopups(message) {
  chrome.runtime.sendMessage(message, () => {
    // Ignore errors - popup might not be open
  });
}

// Store extension state
chrome.storage.sync.get(['settings'], (items) => {
  const settings = items.settings || {
    monitoringEnabled: true,
    captureVisibleOnly: false,
    includeHiddenElements: false
  };

  // Apply settings
  setupMonitoring(settings);
});

function setupMonitoring(settings) {
  // Initialize monitoring based on settings
}
```

---

## DOM Scraping Engine

### Core Scraping Function

The content script extracts page structure and element information:

```javascript
// content.js - DOM scraping engine

const port = chrome.runtime.connect({ name: 'content-script' });

// Global state
let pageSnapshot = null;
let elementIndex = new Map();
let monitoringEnabled = true;
let mutationObserver = null;

// Initialize
initializeContentScript();

function initializeContentScript() {
  // Extract initial page state
  capturePageSnapshot();

  // Start monitoring for changes
  startDOMMonitoring();

  // Listen for messages from background
  port.onMessage.addListener(handleBackgroundMessage);

  // Inject measurement utilities
  injectMeasurementScript();
}

function capturePageSnapshot() {
  const snapshot = {
    timestamp: Date.now(),
    url: window.location.href,
    title: document.title,
    viewport: {
      width: window.innerWidth,
      height: window.innerHeight,
      scrollX: window.scrollX,
      scrollY: window.scrollY
    },
    elements: extractAllElements(),
    metadata: extractPageMetadata()
  };

  pageSnapshot = snapshot;
  elementIndex.clear();

  // Index elements by ID for fast lookup
  snapshot.elements.forEach((elem, index) => {
    elementIndex.set(elem.id, elem);
  });

  return snapshot;
}

function extractAllElements() {
  const elements = [];
  let elementId = 0;

  // Get all elements in document order
  const treeWalker = document.createTreeWalker(
    document.body,
    NodeFilter.SHOW_ELEMENT,
    {
      acceptNode: (node) => {
        // Skip script and style elements
        if (node.tagName === 'SCRIPT' || node.tagName === 'STYLE') {
          return NodeFilter.FILTER_REJECT;
        }
        return NodeFilter.FILTER_ACCEPT;
      }
    }
  );

  let currentNode;
  while (currentNode = treeWalker.nextNode()) {
    const element = extractElementInfo(currentNode, elementId++);
    elements.push(element);
  }

  return elements;
}

function extractElementInfo(element, elementId) {
  const rect = element.getBoundingClientRect();
  const computedStyle = window.getComputedStyle(element);

  // Generate stable ID
  const stableId = generateStableElementId(element);

  return {
    // Identification
    id: stableId,
    domIndex: elementId,
    tagName: element.tagName.toLowerCase(),
    className: element.className,
    resourceId: element.id || null,
    dataAttributes: extractDataAttributes(element),

    // Text content
    text: element.textContent?.trim() || '',
    value: element.value || null,
    placeholder: element.placeholder || null,
    ariaLabel: element.getAttribute('aria-label') || null,
    ariaDescription: element.getAttribute('aria-description') || null,

    // Layout (viewport coordinates)
    bounds: {
      x: rect.left,
      y: rect.top,
      width: rect.width,
      height: rect.height,
      visible: isElementVisible(element, rect)
    },

    // Visibility
    display: computedStyle.display,
    visibility: computedStyle.visibility,
    opacity: computedStyle.opacity,
    pointerEvents: computedStyle.pointerEvents,

    // Interaction properties
    isClickable: isElementClickable(element),
    isEditable: isElementEditable(element),
    isScrollable: isElementScrollable(element),
    isFocusable: isElementFocusable(element),
    isSelected: isElementSelected(element),
    isDisabled: element.disabled || false,

    // Form properties
    inputType: element.type || null,
    isRequired: element.required || false,
    autocomplete: element.autocomplete || null,

    // Link properties
    href: element.href || null,
    target: element.target || null,

    // Image properties
    alt: element.alt || null,
    src: element.src || null,

    // Accessibility
    role: element.getAttribute('role') || null,
    ariaHidden: element.getAttribute('aria-hidden') === 'true',
    tabIndex: element.tabIndex,

    // Parent and children count
    parentTagName: element.parentElement?.tagName.toLowerCase() || null,
    childrenCount: element.children.length,

    // Content type
    contentType: inferContentType(element)
  };
}

function generateStableElementId(element) {
  // Try to use native ID
  if (element.id) {
    return `#${element.id}`;
  }

  // Use CSS selector if possible
  const path = getCSSPath(element);
  return path;
}

function getCSSPath(element) {
  if (element.id) {
    return `#${element.id}`;
  }

  const path = [];
  let current = element;

  while (current && current.nodeType === Node.ELEMENT_NODE) {
    let selector = current.tagName.toLowerCase();

    if (current.id) {
      selector += `#${current.id}`;
      path.unshift(selector);
      break;
    } else {
      // Add class selectors
      if (current.className) {
        selector += `.${current.className.split(/\s+/).join('.')}`;
      }

      // Add nth-child if not unique
      let sibling = current.previousElementSibling;
      let nth = 1;
      while (sibling) {
        if (sibling.tagName.toLowerCase() === selector.split(/[.#]/)[0]) {
          nth++;
        }
        sibling = sibling.previousElementSibling;
      }

      if (nth > 1) {
        selector += `:nth-of-type(${nth})`;
      }

      path.unshift(selector);
    }

    current = current.parentElement;

    // Stop if we've gone high enough
    if (path.length > 5) {
      break;
    }
  }

  return path.join(' > ');
}

function extractDataAttributes(element) {
  const data = {};
  if (element.dataset) {
    Object.entries(element.dataset).forEach(([key, value]) => {
      data[key] = value;
    });
  }
  return data;
}

function isElementVisible(element, rect) {
  if (rect.width === 0 || rect.height === 0) return false;
  if (rect.top >= window.innerHeight || rect.bottom <= 0) return false;
  if (rect.left >= window.innerWidth || rect.right <= 0) return false;

  const computedStyle = window.getComputedStyle(element);
  if (computedStyle.display === 'none' ||
      computedStyle.visibility === 'hidden' ||
      computedStyle.opacity === '0') {
    return false;
  }

  return true;
}

function isElementClickable(element) {
  if (element.onclick || element.getAttribute('onclick')) return true;
  if (element.tagName === 'A' || element.tagName === 'BUTTON') return true;
  if (element.tagName === 'INPUT' && !['hidden', 'text', 'email', 'password'].includes(element.type)) return true;
  if (element.role === 'button' || element.role === 'link') return true;
  if (element.classList.contains('clickable') || element.classList.contains('btn')) return true;

  // Check for event listeners
  return hasEventListeners(element, ['click', 'mousedown', 'touchstart']);
}

function isElementEditable(element) {
  if (element.tagName === 'TEXTAREA') return true;
  if (element.tagName === 'INPUT' && ['text', 'email', 'password', 'url', 'number'].includes(element.type)) return true;
  if (element.contentEditable === 'true') return true;
  if (element.getAttribute('role') === 'textbox' || element.getAttribute('role') === 'searchbox') return true;

  return false;
}

function isElementScrollable(element) {
  return element.scrollHeight > element.clientHeight ||
         element.scrollWidth > element.clientWidth;
}

function isElementFocusable(element) {
  const focusableElements = ['A', 'BUTTON', 'INPUT', 'SELECT', 'TEXTAREA', 'SUMMARY'];
  if (focusableElements.includes(element.tagName)) return true;
  if (element.tabIndex >= 0) return true;

  return false;
}

function isElementSelected(element) {
  if (element.tagName === 'OPTION') return element.selected;
  if (element.tagName === 'INPUT') return element.checked || false;

  return false;
}

function inferContentType(element) {
  if (element.tagName === 'BUTTON' || element.role === 'button') return 'button';
  if (element.tagName === 'A') return 'link';
  if (element.tagName === 'INPUT') return 'input';
  if (element.tagName === 'TEXTAREA') return 'textarea';
  if (element.tagName === 'IMG') return 'image';
  if (element.tagName === 'VIDEO') return 'video';
  if (element.tagName === 'AUDIO') return 'audio';
  if (element.tagName === 'FORM') return 'form';
  if (['H1', 'H2', 'H3', 'H4', 'H5', 'H6'].includes(element.tagName)) return 'heading';
  if (element.tagName === 'P') return 'paragraph';
  if (element.tagName === 'UL' || element.tagName === 'OL') return 'list';
  if (element.tagName === 'TABLE') return 'table';

  return 'generic';
}

function extractPageMetadata() {
  const meta = {};

  // Standard meta tags
  document.querySelectorAll('meta').forEach(tag => {
    const name = tag.getAttribute('name') || tag.getAttribute('property');
    const content = tag.getAttribute('content');
    if (name && content) {
      meta[name] = content;
    }
  });

  // Open Graph
  const ogImage = document.querySelector('meta[property="og:image"]');
  if (ogImage) {
    meta.ogImage = ogImage.getAttribute('content');
  }

  return meta;
}

function hasEventListeners(element, eventTypes) {
  // This is a heuristic - perfect detection is not possible
  // Check for event delegation on parent elements
  for (let parent = element.parentElement; parent; parent = parent.parentElement) {
    const parentHtml = parent.outerHTML;
    for (const eventType of eventTypes) {
      if (parentHtml.includes(`on${eventType}`) || parentHtml.includes(`addEventListener`)) {
        return true;
      }
    }
  }

  return false;
}

// Message handler
function handleBackgroundMessage(request) {
  if (request.action === 'extract-elements') {
    const elements = extractElementsBySelector(request.selector);
    port.postMessage({
      source: 'content-script',
      action: 'extract-result',
      elements: elements
    });
  } else if (request.action === 'execute-action') {
    executeElementAction(request.elementId, request.action).then(result => {
      port.postMessage({
        source: 'content-script',
        action: 'execute-result',
        success: result
      });
    });
  } else if (request.action === 'toggle-monitoring') {
    monitoringEnabled = request.enabled;
  }
}

function extractElementsBySelector(selector) {
  const elements = [];
  document.querySelectorAll(selector).forEach((elem, index) => {
    elements.push(extractElementInfo(elem, index));
  });
  return elements;
}
```

---

## Dynamic Content Handling

### MutationObserver for Real-Time Updates

Detecting changes in single-page applications:

```javascript
// Observe DOM mutations
function startDOMMonitoring() {
  const config = {
    childList: true,      // Watch for added/removed nodes
    subtree: true,        // Watch entire document
    attributes: true,     // Watch attribute changes
    attributeFilter: ['class', 'disabled', 'aria-hidden', 'disabled'],
    characterData: false,
    attributeOldValue: true,
    characterDataOldValue: false,
    subtreeModificationMutation: true
  };

  mutationObserver = new MutationObserver(handleMutations);
  mutationObserver.observe(document.documentElement, config);
}

function handleMutations(mutations) {
  if (!monitoringEnabled) return;

  const changes = [];
  const addedElements = new Set();
  const removedElements = new Set();
  const modifiedElements = new Map();

  for (const mutation of mutations) {
    if (mutation.type === 'childList') {
      // Track added nodes
      mutation.addedNodes.forEach(node => {
        if (node.nodeType === Node.ELEMENT_NODE) {
          addedElements.add(node);

          // Also extract info from descendants
          node.querySelectorAll('*').forEach(elem => {
            addedElements.add(elem);
          });
        }
      });

      // Track removed nodes
      mutation.removedNodes.forEach(node => {
        if (node.nodeType === Node.ELEMENT_NODE) {
          removedElements.add(node);
        }
      });

    } else if (mutation.type === 'attributes') {
      // Track attribute changes
      const target = mutation.target;
      const attrName = mutation.attributeName;
      const oldValue = mutation.oldValue;
      const newValue = target.getAttribute(attrName);

      if (!modifiedElements.has(target)) {
        modifiedElements.set(target, []);
      }

      modifiedElements.get(target).push({
        attribute: attrName,
        oldValue: oldValue,
        newValue: newValue
      });
    }
  }

  // Compile and send changes
  const elementChanges = {
    added: Array.from(addedElements).map((elem, idx) =>
      extractElementInfo(elem, idx)
    ),
    removed: Array.from(removedElements).map(elem => ({
      id: generateStableElementId(elem),
      tagName: elem.tagName.toLowerCase()
    })),
    modified: Array.from(modifiedElements.entries()).map(([elem, attrs]) => ({
      id: generateStableElementId(elem),
      attributeChanges: attrs,
      currentElement: extractElementInfo(elem)
    }))
  };

  if (elementChanges.added.length > 0 ||
      elementChanges.removed.length > 0 ||
      elementChanges.modified.length > 0) {

    port.postMessage({
      source: 'content-script',
      action: 'page-changed',
      changes: elementChanges
    });
  }
}

// Detect and track SPA navigation
const originalPushState = window.history.pushState;
const originalReplaceState = window.history.replaceState;

window.history.pushState = function(...args) {
  originalPushState.apply(this, args);
  onRouteChange();
};

window.history.replaceState = function(...args) {
  originalReplaceState.apply(this, args);
  onRouteChange();
};

window.addEventListener('popstate', onRouteChange);

function onRouteChange() {
  // Wait for DOM to update
  setTimeout(() => {
    const newSnapshot = capturePageSnapshot();

    port.postMessage({
      source: 'content-script',
      action: 'route-changed',
      snapshot: newSnapshot
    });
  }, 100);
}

// Detect fetch/XHR completions for async content
const originalFetch = window.fetch;
window.fetch = function(...args) {
  return originalFetch.apply(this, args).then(response => {
    // Check if response modifies DOM
    setTimeout(() => {
      const newSnapshot = capturePageSnapshot();
      const changes = detectSnapshotChanges(pageSnapshot, newSnapshot);

      if (changes.hasChanges) {
        port.postMessage({
          source: 'content-script',
          action: 'content-loaded',
          snapshot: newSnapshot
        });
      }
    }, 50);

    return response;
  });
};

function detectSnapshotChanges(oldSnapshot, newSnapshot) {
  if (!oldSnapshot || !newSnapshot) return { hasChanges: false };

  const oldElements = new Set(oldSnapshot.elements.map(e => e.id));
  const newElements = new Set(newSnapshot.elements.map(e => e.id));

  const hasChanges = oldElements.size !== newElements.size ||
    Array.from(oldElements).some(id => !newElements.has(id));

  return { hasChanges };
}
```

### Form Change Detection

Tracking user input and form modifications:

```javascript
// Track form input changes
document.addEventListener('input', (event) => {
  if (event.target.tagName === 'INPUT' || event.target.tagName === 'TEXTAREA') {
    port.postMessage({
      source: 'content-script',
      action: 'form-input-changed',
      elementId: generateStableElementId(event.target),
      value: event.target.value
    });
  }
});

// Track form submissions
document.addEventListener('submit', (event) => {
  const form = event.target;

  port.postMessage({
    source: 'content-script',
    action: 'form-submitted',
    formId: generateStableElementId(form),
    values: new FormData(form)
  });
});

// Track focus changes
document.addEventListener('focus', (event) => {
  if (event.target.tagName === 'INPUT' || event.target.tagName === 'TEXTAREA') {
    port.postMessage({
      source: 'content-script',
      action: 'element-focused',
      elementId: generateStableElementId(event.target)
    });
  }
}, true);
```

---

## Cross-Browser Support

### Browser Detection and Adaptation

```javascript
// Utility for cross-browser compatibility
const BrowserCompat = {
  isChrome: () => /Chrome/.test(navigator.userAgent),
  isFirefox: () => /Firefox/.test(navigator.userAgent),
  isSafari: () => /Safari/.test(navigator.userAgent),
  isEdge: () => /Edg/.test(navigator.userAgent),

  sendMessage: function(data) {
    if (this.isFirefox()) {
      // Firefox uses slightly different messaging
      return new Promise((resolve, reject) => {
        browser.runtime.sendMessage(data).then(resolve).catch(reject);
      });
    } else {
      // Chrome, Edge, Safari
      return new Promise((resolve, reject) => {
        chrome.runtime.sendMessage(data, (response) => {
          if (chrome.runtime.lastError) {
            reject(chrome.runtime.lastError);
          } else {
            resolve(response);
          }
        });
      });
    }
  },

  getStorageAPI: function() {
    if (this.isFirefox()) {
      return browser.storage;
    } else {
      return chrome.storage;
    }
  },

  registerContentPort: function(name) {
    if (this.isFirefox()) {
      return browser.runtime.connect({ name });
    } else {
      return chrome.runtime.connect({ name });
    }
  }
};

// Use in content script
const port = BrowserCompat.registerContentPort('content-script');
```

### Browser-Specific Manifests

**manifest.json (Manifest V3 - Chrome/Edge):**
Already defined above.

**manifest.json (Firefox - Manifest V2):**

```json
{
  "manifest_version": 2,
  "name": "VOS4 Web Assistant",
  "version": "1.0.0",
  "permissions": [
    "tabs",
    "activeTab",
    "<all_urls>",
    "storage",
    "webRequest"
  ],
  "background": {
    "scripts": ["background.js"]
  },
  "content_scripts": [
    {
      "matches": ["<all_urls>"],
      "js": ["content.js"],
      "run_at": "document_start",
      "all_frames": true
    }
  ],
  "browser_action": {
    "default_popup": "popup.html",
    "default_title": "VOS4 Web Assistant",
    "default_icon": "icons/icon-48.png"
  },
  "icons": {
    "16": "icons/icon-16.png",
    "48": "icons/icon-48.png",
    "128": "icons/icon-128.png"
  },
  "native_messaging": true,
  "description": "Voice-driven web interaction for VOS4"
}
```

**Safari Web Extension (Info.plist):**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>NSExtension</key>
  <dict>
    <key>NSExtensionDisplayName</key>
    <string>VOS4 Web Assistant</string>
    <key>NSExtensionIdentifier</key>
    <string>com.augmentalis.vos4.web-assistant</string>
    <key>NSExtensionPointIdentifier</key>
    <string>com.apple.Safari.web-extension</string>
    <key>NSExtensionPointVersion</key>
    <string>1</string>
    <key>SFSafariContentScript</key>
    <array>
      <dict>
        <key>Script</key>
        <string>content.js</string>
        <key>AllFrames</key>
        <true/>
      </dict>
    </array>
  </dict>
</dict>
</plist>
```

---

## Native App Integration

### Native Messaging Protocol

Communication between browser extension and native app (VOS4):

```javascript
// native-messaging.js
class NativeAppMessenger {
  constructor(appId = 'com.augmentalis.vos4.webassistant') {
    this.appId = appId;
    this.connected = false;
    this.messageQueue = [];
    this.requestId = 0;
    this.responseCallbacks = new Map();

    this.connect();
  }

  connect() {
    try {
      this.port = chrome.runtime.connectNative(this.appId);
      this.connected = true;

      this.port.onMessage.addListener((message) => {
        this.handleMessage(message);
      });

      this.port.onDisconnect.addListener(() => {
        this.connected = false;
        console.log('Disconnected from native app');
        this.reconnectSoon();
      });

      // Send queued messages
      while (this.messageQueue.length > 0) {
        const message = this.messageQueue.shift();
        this.sendRaw(message);
      }

    } catch (error) {
      console.error('Failed to connect to native app:', error);
      this.reconnectSoon();
    }
  }

  reconnectSoon() {
    setTimeout(() => this.connect(), 5000);
  }

  send(message) {
    const requestId = this.requestId++;
    const envelope = {
      requestId: requestId,
      ...message
    };

    if (this.connected) {
      this.sendRaw(envelope);
    } else {
      this.messageQueue.push(envelope);
    }

    return new Promise((resolve, reject) => {
      this.responseCallbacks.set(requestId, { resolve, reject });

      // Timeout after 10 seconds
      setTimeout(() => {
        if (this.responseCallbacks.has(requestId)) {
          this.responseCallbacks.delete(requestId);
          reject(new Error('Native app response timeout'));
        }
      }, 10000);
    });
  }

  sendRaw(message) {
    try {
      this.port.postMessage(message);
    } catch (error) {
      console.error('Failed to send message to native app:', error);
      this.messageQueue.push(message);
    }
  }

  handleMessage(message) {
    if (message.requestId !== undefined) {
      const callback = this.responseCallbacks.get(message.requestId);
      if (callback) {
        this.responseCallbacks.delete(message.requestId);
        if (message.error) {
          callback.reject(new Error(message.error));
        } else {
          callback.resolve(message.response);
        }
      }
    }
  }

  async extractPage() {
    return this.send({
      action: 'extract-page',
      data: {
        url: window.location.href,
        title: document.title
      }
    });
  }

  async executeCommand(command, target) {
    return this.send({
      action: 'execute-command',
      data: {
        command: command,
        target: target,
        url: window.location.href
      }
    });
  }
}

// Global instance
const nativeMessenger = new NativeAppMessenger();

// Expose to content scripts
window.vos4NativeMessenger = nativeMessenger;
```

### Native App Handler (Kotlin)

```kotlin
// modules/apps/WebAssistant/src/main/kotlin/NativeMessagingHost.kt

class NativeMessagingHost {

    private val inputStream = System.`in`
    private val outputStream = System.out

    fun start() {
        while (true) {
            try {
                val message = readMessage()
                if (message == null) break

                val response = handleMessage(message)
                sendMessage(response)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun readMessage(): JSONObject? {
        val lengthBytes = ByteArray(4)
        val bytesRead = inputStream.read(lengthBytes)
        if (bytesRead != 4) return null

        val length = ByteBuffer.wrap(lengthBytes)
            .order(ByteOrder.LITTLE_ENDIAN)
            .int

        val messageBytes = ByteArray(length)
        inputStream.read(messageBytes)

        val messageStr = String(messageBytes, Charsets.UTF_8)
        return JSONObject(messageStr)
    }

    private fun sendMessage(message: JSONObject) {
        val bytes = message.toString().toByteArray(Charsets.UTF_8)
        val length = bytes.size

        val lengthBytes = ByteBuffer.allocate(4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(length)
            .array()

        outputStream.write(lengthBytes)
        outputStream.write(bytes)
        outputStream.flush()
    }

    private fun handleMessage(message: JSONObject): JSONObject {
        val requestId = message.optInt("requestId", 0)
        val action = message.optString("action")

        val response = JSONObject().put("requestId", requestId)

        try {
            when (action) {
                "extract-page" -> {
                    val pageData = message.optJSONObject("data")
                    val result = extractPageData(pageData)
                    response.put("response", result)
                }

                "execute-command" -> {
                    val commandData = message.optJSONObject("data")
                    val command = commandData.optString("command")
                    val target = commandData.optString("target")

                    executeVoiceCommand(command, target)
                    response.put("response", JSONObject().put("success", true))
                }

                else -> {
                    response.put("error", "Unknown action: $action")
                }
            }
        } catch (e: Exception) {
            response.put("error", e.message)
        }

        return response
    }

    private fun extractPageData(pageData: JSONObject?): JSONObject {
        // Process page data from extension
        // Return analyzed results
        return JSONObject()
    }

    private fun executeVoiceCommand(command: String, target: String) {
        // Execute voice command in VOS4
    }
}
```

---

## Security & CSP

### Content Security Policy

Secure configuration that respects CSP:

```javascript
// inject.js - Injected script that runs in page context
// This allows us to interact with the page's JavaScript APIs

(function() {
  // Create a communication bridge between content script and injected script
  const channel = new BroadcastChannel('vos4-injection');

  window.vos4 = {
    getFormData: function(formElement) {
      return new FormData(formElement);
    },

    executeScript: function(code) {
      // Only execute safe operations
      try {
        return Function('"use strict"; return (' + code + ')')();
      } catch (e) {
        return { error: e.message };
      }
    }
  };

  // Listen for requests from content script
  channel.onmessage = (event) => {
    const { action, data } = event.data;
    let result;

    if (action === 'get-form-data') {
      const form = document.querySelector(data.selector);
      result = new FormData(form);
    }

    channel.postMessage({ action, result });
  };
})();
```

### CSP-Compliant Content Script

```javascript
// content.js with CSP compliance

// Instead of eval or new Function, use event-based communication
function executeInPageContext(code) {
  return new Promise((resolve, reject) => {
    const channel = new BroadcastChannel('vos4-injection');

    channel.onmessage = (event) => {
      resolve(event.data.result);
    };

    channel.postMessage({
      action: 'execute-script',
      data: { code }
    });

    // Timeout
    setTimeout(() => reject(new Error('Timeout')), 5000);
  });
}

// Safe DOM manipulation without eval
function querySelector(selector) {
  try {
    return document.querySelector(selector);
  } catch (e) {
    return null;
  }
}

function querySelectorAll(selector) {
  try {
    return Array.from(document.querySelectorAll(selector));
  } catch (e) {
    return [];
  }
}
```

### CORS Handling

```javascript
// Proxy requests through native app to avoid CORS
async function fetchWithNativeProxy(url, options) {
  return nativeMessenger.send({
    action: 'fetch',
    data: {
      url: url,
      options: options
    }
  });
}

// Use instead of direct fetch for cross-origin requests
async function getRemoteData(url) {
  try {
    const response = await fetchWithNativeProxy(url);
    return response;
  } catch (error) {
    console.error('Fetch error:', error);
    return null;
  }
}
```

---

## JavaScript Examples

### Example 1: Extract All Links and Buttons

```javascript
async function extractInteractiveElements() {
  const scraper = {
    links: [],
    buttons: [],
    forms: []
  };

  // Find all links
  document.querySelectorAll('a').forEach((link, index) => {
    scraper.links.push({
      id: `link-${index}`,
      text: link.textContent,
      href: link.href,
      title: link.title,
      x: link.getBoundingClientRect().left,
      y: link.getBoundingClientRect().top
    });
  });

  // Find all buttons
  document.querySelectorAll('button').forEach((button, index) => {
    scraper.buttons.push({
      id: `button-${index}`,
      text: button.textContent,
      type: button.type,
      disabled: button.disabled,
      x: button.getBoundingClientRect().left,
      y: button.getBoundingClientRect().top
    });
  });

  // Find all forms
  document.querySelectorAll('form').forEach((form, index) => {
    const inputs = [];
    form.querySelectorAll('input, textarea, select').forEach((input) => {
      inputs.push({
        name: input.name,
        type: input.type,
        value: input.value,
        required: input.required
      });
    });

    scraper.forms.push({
      id: `form-${index}`,
      action: form.action,
      method: form.method,
      inputs: inputs
    });
  });

  return scraper;
}

// Usage
const elements = await extractInteractiveElements();
port.postMessage({
  source: 'content-script',
  action: 'interactive-elements',
  data: elements
});
```

### Example 2: Respond to Voice Commands

```javascript
class VoiceCommandExecutor {
  constructor() {
    this.port = chrome.runtime.connect({ name: 'content-script' });
  }

  async executeCommand(command, targetText) {
    const target = await this.findElement(targetText);
    if (!target) {
      return { success: false, error: `Could not find "${targetText}"` };
    }

    return this.performAction(target, command);
  }

  async findElement(text) {
    // Search for element by text
    const allElements = Array.from(document.querySelectorAll('*'));

    return allElements.find(elem => {
      const elemText = elem.textContent.toLowerCase().trim();
      const searchText = text.toLowerCase().trim();

      // Exact match
      if (elemText === searchText) return true;

      // Partial match
      if (elemText.includes(searchText)) return true;

      // Check aria-label
      const ariaLabel = elem.getAttribute('aria-label');
      if (ariaLabel && ariaLabel.toLowerCase().includes(searchText)) return true;

      return false;
    });
  }

  async performAction(element, command) {
    switch (command.toLowerCase()) {
      case 'click':
      case 'tap':
        element.click();
        return { success: true };

      case 'focus':
        if (typeof element.focus === 'function') {
          element.focus();
          return { success: true };
        }
        break;

      case 'type':
        if (element.tagName === 'INPUT' || element.tagName === 'TEXTAREA') {
          element.focus();
          return { success: true };
        }
        break;

      case 'scroll-into-view':
        element.scrollIntoView({ behavior: 'smooth' });
        return { success: true };

      default:
        return { success: false, error: `Unknown command: ${command}` };
    }

    return { success: false };
  }
}

// Usage
const executor = new VoiceCommandExecutor();

// "click search button"
executor.executeCommand('click', 'search').then(result => {
  console.log(result);
});

// "type hello" (on focused input)
executor.executeCommand('type', 'hello').then(result => {
  console.log(result);
});
```

### Example 3: Real-Time Page Monitoring

```javascript
class PageMonitor {
  constructor() {
    this.lastSnapshot = null;
    this.listeners = [];
  }

  onPageChange(callback) {
    this.listeners.push(callback);
  }

  start() {
    // Initial snapshot
    this.lastSnapshot = this.captureSnapshot();

    // Monitor for changes
    const observer = new MutationObserver(() => {
      const current = this.captureSnapshot();

      if (!this.isEqual(this.lastSnapshot, current)) {
        this.listeners.forEach(cb => cb(current));
        this.lastSnapshot = current;
      }
    });

    observer.observe(document.documentElement, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ['class', 'disabled', 'aria-hidden']
    });
  }

  captureSnapshot() {
    return {
      timestamp: Date.now(),
      elementCount: document.querySelectorAll('*').length,
      focusedElement: document.activeElement?.tagName || null,
      formInputs: Array.from(document.querySelectorAll('input, textarea'))
        .map(input => ({
          name: input.name,
          value: input.value
        }))
    };
  }

  isEqual(a, b) {
    if (!a || !b) return false;
    return a.elementCount === b.elementCount &&
           JSON.stringify(a.formInputs) === JSON.stringify(b.formInputs);
  }
}

// Usage
const monitor = new PageMonitor();

monitor.onPageChange((snapshot) => {
  console.log('Page changed:', snapshot);

  // Send to native app
  nativeMessenger.send({
    action: 'page-changed',
    data: snapshot
  });
});

monitor.start();
```

---

## Testing & Debugging

### Unit Testing

```javascript
// test/dom-scraper.test.js
describe('DOM Scraper', () => {
  beforeEach(() => {
    // Setup DOM
    document.body.innerHTML = `
      <div id="container">
        <button id="btn-submit">Submit</button>
        <input id="input-name" type="text" placeholder="Name" />
        <textarea id="textarea-message">Message</textarea>
      </div>
    `;
  });

  test('extracts button element', () => {
    const elem = extractElementInfo(document.getElementById('btn-submit'));

    expect(elem.tagName).toBe('button');
    expect(elem.text).toBe('Submit');
    expect(elem.isClickable).toBe(true);
  });

  test('identifies input element as editable', () => {
    const elem = extractElementInfo(document.getElementById('input-name'));

    expect(elem.isEditable).toBe(true);
    expect(elem.inputType).toBe('text');
  });

  test('generates stable IDs consistently', () => {
    const elem = document.getElementById('btn-submit');
    const id1 = generateStableElementId(elem);
    const id2 = generateStableElementId(elem);

    expect(id1).toBe(id2);
  });
});
```

### Manual Testing Checklist

- [ ] Extract complete page hierarchy
- [ ] Identify clickable elements accurately
- [ ] Track form input changes
- [ ] Monitor SPA navigation
- [ ] Detect dynamic content (AJAX)
- [ ] Execute actions on elements
- [ ] Handle visibility/hidden elements
- [ ] Support cross-frame iframes
- [ ] Respect accessibility attributes
- [ ] Test performance with large pages

---

## Deployment Guide

### Building the Extension

```bash
# Directory structure
web-scraper-extension/
├── manifest.json
├── background.js
├── content.js
├── inject.js
├── popup.html
├── popup.js
├── styles.css
├── icons/
│   ├── icon-16.png
│   ├── icon-48.png
│   └── icon-128.png
└── test/
```

### Chrome Web Store Submission

1. Create extension.zip with all files
2. Register as Chrome Web Store developer
3. Upload and submit for review
4. Configure permissions and privacy policy

### Firefox AMO Submission

1. Build as XPI file
2. Register with Mozilla Developer Hub
3. Submit for review
4. Comply with Firefox standards

### Safari App Store

1. Wrap as macOS app using Xcode
2. Submit to Apple App Store
3. Obtain Developer Certificate

### Local Testing

```bash
# Chrome/Edge
1. chrome://extensions/
2. Enable "Developer mode"
3. Click "Load unpacked"
4. Select extension directory

# Firefox
1. about:debugging#/runtime/this-firefox
2. Click "Load Temporary Add-on"
3. Select manifest.json

# Safari
1. Develop menu > Show Extension Builder
2. Add extension
3. Allow in Safari settings
```

---

## Summary

The Web Scraping Tool provides comprehensive DOM extraction and manipulation for web-based voice interactions. Key features include:

- **Cross-browser compatibility** via unified API
- **Real-time change detection** for SPAs and dynamic content
- **Secure native integration** via messaging protocol
- **Performance optimization** through caching and incremental updates
- **Voice command execution** on page elements

Integration with native VOS4 enables seamless voice control across both native apps and web interfaces, creating a unified voice-driven experience across all platforms.

---

**Previous Chapter:** Chapter 26 - Native UI Scraping (Cross-Platform)
