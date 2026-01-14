# AVAMagic Studio - Hybrid Architecture Specification

**Version:** 1.0.0
**Date:** 2025-11-21
**Author:** Manoj Jhawar, manoj@ideahq.net
**Status:** Design Specification
**Framework:** IDEACODE 8.4

---

## Executive Summary

This document provides the **detailed technical specification** for the Hybrid Architecture approach: an IntelliJ IDEA/Android Studio plugin that embeds a web-based visual designer. This architecture combines the best of both worlds:

- **Native IDE integration** via IntelliJ Platform SDK (Kotlin)
- **Modern visual designer** via embedded web UI (React + TypeScript)

**Key Innovation:** The web UI is reusable across multiple platforms (VS Code, standalone Tauri app, cloud SaaS), maximizing development efficiency.

---

## Architecture Overview

### High-Level Architecture

```
┌──────────────────────────────────────────────────────────────┐
│              IntelliJ IDEA / Android Studio                  │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              IntelliJ Plugin (Kotlin)                  │ │
│  │                                                        │ │
│  │  ┌──────────────────────────────────────────────────┐ │ │
│  │  │         IDE Integration Layer                    │ │ │
│  │  │  ┌────────────────┐  ┌────────────────────────┐ │ │ │
│  │  │  │  PSI Access    │  │  File Operations       │ │ │ │
│  │  │  │  (Code Tree)   │  │  (Read/Write/Watch)    │ │ │ │
│  │  │  └────────────────┘  └────────────────────────┘ │ │ │
│  │  │  ┌────────────────┐  ┌────────────────────────┐ │ │ │
│  │  │  │  Refactoring   │  │  Build Integration     │ │ │ │
│  │  │  │  Hooks         │  │  (Gradle/Maven)        │ │ │ │
│  │  │  └────────────────┘  └────────────────────────┘ │ │ │
│  │  └──────────────────────────────────────────────────┘ │ │
│  │                                                        │ │
│  │  ┌──────────────────────────────────────────────────┐ │ │
│  │  │       Embedded Web Server (Ktor)                 │ │ │
│  │  │  ┌────────────────┐  ┌────────────────────────┐ │ │ │
│  │  │  │  HTTP/REST API │  │  WebSocket Server      │ │ │ │
│  │  │  │  (Port: random)│  │  (Live Updates)        │ │ │ │
│  │  │  └────────────────┘  └────────────────────────┘ │ │ │
│  │  │  ┌────────────────┐  ┌────────────────────────┐ │ │ │
│  │  │  │  Component API │  │  File Operations API   │ │ │ │
│  │  │  │  (Get/Create)  │  │  (Save/Load)           │ │ │ │
│  │  │  └────────────────┘  └────────────────────────┘ │ │ │
│  │  └──────────────────────────────────────────────────┘ │ │
│  │                                                        │ │
│  │  ┌──────────────────────────────────────────────────┐ │ │
│  │  │           JCEF Browser (Chromium)                │ │ │
│  │  │                                                  │ │ │
│  │  │  ┌────────────────────────────────────────────┐ │ │ │
│  │  │  │         React Web UI (TypeScript)         │ │ │ │
│  │  │  │                                            │ │ │ │
│  │  │  │  ┌────────────┐  ┌────────────────────┐  │ │ │ │
│  │  │  │  │   Canvas   │  │  Component Palette │  │ │ │ │
│  │  │  │  │  (Drag &   │  │  (48 Components)   │  │ │ │ │
│  │  │  │  │   Drop)    │  │                    │  │ │ │ │
│  │  │  │  └────────────┘  └────────────────────┘  │ │ │ │
│  │  │  │  ┌────────────┐  ┌────────────────────┐  │ │ │ │
│  │  │  │  │ Property   │  │  Live Preview      │  │ │ │ │
│  │  │  │  │ Inspector  │  │  (7 Platforms)     │  │ │ │ │
│  │  │  │  └────────────┘  └────────────────────┘  │ │ │ │
│  │  │  │  ┌─────────────────────────────────────┐ │ │ │ │
│  │  │  │  │      Theme Designer                 │ │ │ │ │
│  │  │  │  └─────────────────────────────────────┘ │ │ │ │
│  │  │  └────────────────────────────────────────────┘ │ │ │
│  │  │                                                  │ │ │
│  │  └──────────────────────────────────────────────────┘ │ │
│  │                 ↕                                      │ │
│  │          REST/WebSocket                               │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              Native IDE UI (Swing)                     │ │
│  │  - Syntax Highlighting                                 │ │
│  │  - Code Completion                                     │ │
│  │  - Refactoring Tools                                   │ │
│  └────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

---

## Component Breakdown

### 1. IntelliJ Plugin Layer (Kotlin)

#### 1.1 IDE Integration

**Responsibilities:**
- Access to Project Structure Interface (PSI)
- File system operations (read, write, watch)
- Integration with IntelliJ's refactoring system
- Build tool integration (Gradle, Maven)
- Version control hooks (Git, SVN)

**Key Classes:**

```kotlin
// AVAMagicPlugin.kt
class AVAMagicPlugin : Plugin {
    private lateinit var webServer: EmbeddedWebServer

    override fun initComponent() {
        // Start embedded web server
        webServer = EmbeddedWebServer()
        webServer.start()
    }

    override fun disposeComponent() {
        webServer.stop()
    }
}

// PSIBridge.kt
class PSIBridge(private val project: Project) {
    fun getComponentTree(file: VirtualFile): ComponentTree {
        val psiFile = PsiManager.getInstance(project).findFile(file)
        return AVAMagicPSIParser.parse(psiFile)
    }

    fun updateComponent(
        file: VirtualFile,
        componentId: String,
        newProperties: Map<String, Any>
    ) {
        ApplicationManager.getApplication().runWriteAction {
            val psiFile = PsiManager.getInstance(project).findFile(file)
            // Update PSI tree
            AVAMagicPSIUpdater.updateComponent(psiFile, componentId, newProperties)
        }
    }
}

// FileOperationsBridge.kt
class FileOperationsBridge(private val project: Project) {
    suspend fun readFile(path: String): String {
        return ApplicationManager.getApplication().runReadAction<String> {
            val file = LocalFileSystem.getInstance().findFileByPath(path)
            VfsUtil.loadText(file!!)
        }
    }

    suspend fun writeFile(path: String, content: String) {
        ApplicationManager.getApplication().runWriteAction {
            val file = LocalFileSystem.getInstance().findFileByPath(path)
            VfsUtil.saveText(file!!, content)
        }
    }

    fun watchFile(path: String, callback: (String) -> Unit): FileWatcher {
        return FileWatcher(path) { event ->
            when (event.type) {
                FileEvent.Type.MODIFIED -> callback(event.path)
                else -> {}
            }
        }
    }
}
```

#### 1.2 Embedded Web Server

**Technology:** Ktor 2.3+
**Port:** Random available port (avoid conflicts)
**Protocol:** HTTP/REST + WebSockets

**Key Features:**
- Serve static web UI assets (React build)
- REST API for CRUD operations
- WebSocket for live updates
- CORS enabled (localhost only)

**Implementation:**

```kotlin
// EmbeddedWebServer.kt
class EmbeddedWebServer {
    private var server: ApplicationEngine? = null
    private var port: Int = 0

    fun start(): Int {
        port = findAvailablePort()

        server = embeddedServer(Netty, port = port) {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(15)
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }

            install(CORS) {
                allowHost("localhost:$port")
                allowMethod(HttpMethod.Get)
                allowMethod(HttpMethod.Post)
                allowMethod(HttpMethod.Put)
                allowMethod(HttpMethod.Delete)
            }

            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }

            routing {
                // Serve static React app
                static("/") {
                    resources("web-ui")
                    defaultResource("index.html", "web-ui")
                }

                // Component API
                route("/api") {
                    get("/components") {
                        val components = ComponentRegistry.getAll()
                        call.respond(components)
                    }

                    get("/components/{id}") {
                        val id = call.parameters["id"]!!
                        val component = ComponentRegistry.get(id)
                        call.respond(component ?: HttpStatusCode.NotFound)
                    }

                    post("/components") {
                        val request = call.receive<CreateComponentRequest>()
                        val component = ComponentService.create(request)
                        call.respond(HttpStatusCode.Created, component)
                    }

                    put("/components/{id}") {
                        val id = call.parameters["id"]!!
                        val request = call.receive<UpdateComponentRequest>()
                        ComponentService.update(id, request)
                        call.respond(HttpStatusCode.OK)
                    }
                }

                // File Operations API
                route("/api/files") {
                    get("/{path...}") {
                        val path = call.parameters.getAll("path")!!.joinToString("/")
                        val content = FileOperationsBridge(project).readFile(path)
                        call.respondText(content)
                    }

                    post("/{path...}") {
                        val path = call.parameters.getAll("path")!!.joinToString("/")
                        val content = call.receiveText()
                        FileOperationsBridge(project).writeFile(path, content)
                        call.respond(HttpStatusCode.OK)
                    }
                }

                // WebSocket for live updates
                webSocket("/ws") {
                    val session = WebSocketSession(this)
                    WebSocketManager.register(session)

                    try {
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> {
                                    val message = Json.decodeFromString<WSMessage>(frame.readText())
                                    handleWebSocketMessage(message, session)
                                }
                                else -> {}
                            }
                        }
                    } finally {
                        WebSocketManager.unregister(session)
                    }
                }
            }
        }.start(wait = false)

        return port
    }

    fun stop() {
        server?.stop(1000, 5000)
    }

    private fun findAvailablePort(): Int {
        val socket = ServerSocket(0)
        val port = socket.localPort
        socket.close()
        return port
    }

    private suspend fun handleWebSocketMessage(message: WSMessage, session: WebSocketSession) {
        when (message.type) {
            "component_updated" -> {
                // Broadcast to other sessions
                WebSocketManager.broadcast(message, exclude = session)
            }
            "file_changed" -> {
                // Trigger file reload
                FileOperationsBridge(project).watchFile(message.data["path"] as String) {
                    session.send(WSMessage("file_reloaded", mapOf("path" to it)))
                }
            }
            else -> {
                Logger.warn("Unknown WebSocket message type: ${message.type}")
            }
        }
    }
}

// WebSocketManager.kt
object WebSocketManager {
    private val sessions = mutableSetOf<WebSocketSession>()

    fun register(session: WebSocketSession) {
        sessions.add(session)
    }

    fun unregister(session: WebSocketSession) {
        sessions.remove(session)
    }

    suspend fun broadcast(message: WSMessage, exclude: WebSocketSession? = null) {
        sessions.filter { it != exclude }.forEach { session ->
            session.send(message)
        }
    }
}

data class WSMessage(
    val type: String,
    val data: Map<String, Any>
)
```

#### 1.3 JCEF Integration

**JCEF:** Java Chromium Embedded Framework (Replaces deprecated JavaFX)

**Key Classes:**

```kotlin
// ComponentDesignerToolWindow.kt
class ComponentDesignerToolWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val server = AVAMagicPlugin.getInstance().getWebServer()
        val url = "http://localhost:${server.port}"

        // Create JCEF browser
        val browser = JBCefBrowser.createBuilder()
            .setUrl(url)
            .setEnableOpenDevToolsMenuItem(true) // Allow Chrome DevTools
            .build()

        // Set up JavaScript bridge (optional, for direct communication)
        val jsBridge = JBCefJSQuery.create(browser as JBCefBrowserBase)
        jsBridge.addHandler { request ->
            handleJavaScriptCall(request)
            null
        }

        // Create panel with browser
        val contentPanel = JPanel(BorderLayout())
        contentPanel.add(browser.component, BorderLayout.CENTER)

        // Add toolbar (optional)
        val toolbar = createToolbar(browser)
        contentPanel.add(toolbar, BorderLayout.NORTH)

        // Register with tool window
        val content = ContentFactory.getInstance()
            .createContent(contentPanel, "Visual Designer", false)
        toolWindow.contentManager.addContent(content)

        // Clean up on dispose
        Disposer.register(toolWindow.disposable) {
            browser.dispose()
        }
    }

    private fun createToolbar(browser: JBCefBrowser): JComponent {
        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT))

        // Reload button
        val reloadButton = JButton("Reload", AllIcons.Actions.Refresh).apply {
            addActionListener {
                browser.cefBrowser.reload()
            }
        }
        toolbar.add(reloadButton)

        // Dev Tools button
        val devToolsButton = JButton("DevTools", AllIcons.Actions.StartDebugger).apply {
            addActionListener {
                browser.openDevtools()
            }
        }
        toolbar.add(devToolsButton)

        return toolbar
    }

    private fun handleJavaScriptCall(request: String): String {
        // Handle direct JavaScript → Kotlin calls
        // (Alternative to REST API for performance-critical operations)
        val json = Json.parseToJsonElement(request).jsonObject
        return when (json["action"]?.jsonPrimitive?.content) {
            "getProject" -> {
                Json.encodeToString(ProjectInfo(project))
            }
            else -> "{}"
        }
    }
}

// JCEFResourceHandler.kt
class JCEFResourceHandler : CefResourceHandler {
    override fun processRequest(request: CefRequest, callback: CefCallback): Boolean {
        val url = request.url

        // Map URLs to local resources
        val resourcePath = when {
            url.endsWith(".js") -> "web-ui/static/js/${extractFileName(url)}"
            url.endsWith(".css") -> "web-ui/static/css/${extractFileName(url)}"
            url.endsWith(".html") -> "web-ui/index.html"
            else -> "web-ui/index.html"
        }

        // Load resource from JAR
        val inputStream = javaClass.classLoader.getResourceAsStream(resourcePath)
        if (inputStream != null) {
            responseData = inputStream.readBytes()
            callback.Continue()
            return true
        }

        callback.cancel()
        return false
    }

    // ... other CefResourceHandler methods
}
```

---

### 2. React Web UI Layer (TypeScript)

#### 2.1 Project Structure

```
web-ui/
├── public/
│   ├── index.html
│   └── favicon.ico
├── src/
│   ├── api/
│   │   ├── PluginAPIClient.ts      # REST API client
│   │   ├── WebSocketClient.ts      # WebSocket client
│   │   └── types.ts                # API type definitions
│   ├── components/
│   │   ├── Canvas/
│   │   │   ├── DropCanvas.tsx      # Main drag-drop canvas
│   │   │   ├── ComponentRenderer.tsx
│   │   │   ├── SelectionOverlay.tsx
│   │   │   ├── GridOverlay.tsx
│   │   │   └── ZoomControls.tsx
│   │   ├── Palette/
│   │   │   ├── ComponentPalette.tsx
│   │   │   ├── CategoryTabs.tsx
│   │   │   ├── ComponentCard.tsx
│   │   │   └── SearchBar.tsx
│   │   ├── Inspector/
│   │   │   ├── PropertyInspector.tsx
│   │   │   ├── PropertyGroup.tsx
│   │   │   ├── editors/
│   │   │   │   ├── StringEditor.tsx
│   │   │   │   ├── NumberEditor.tsx
│   │   │   │   ├── ColorEditor.tsx
│   │   │   │   ├── EnumEditor.tsx
│   │   │   │   └── ...
│   │   │   └── PropertyValidation.ts
│   │   ├── Preview/
│   │   │   ├── LivePreview.tsx
│   │   │   ├── PlatformSelector.tsx
│   │   │   └── PreviewIframe.tsx
│   │   ├── Theme/
│   │   │   ├── ThemeDesigner.tsx
│   │   │   ├── ColorPalette.tsx
│   │   │   └── TypographyEditor.tsx
│   │   └── common/
│   │       ├── Button.tsx
│   │       ├── Input.tsx
│   │       └── ...
│   ├── state/
│   │   ├── designerStore.ts        # Zustand store
│   │   ├── selectors.ts
│   │   └── actions.ts
│   ├── hooks/
│   │   ├── useAPI.ts
│   │   ├── useWebSocket.ts
│   │   ├── useDragDrop.ts
│   │   └── useKeyboardShortcuts.ts
│   ├── utils/
│   │   ├── dslParser.ts
│   │   ├── dslGenerator.ts
│   │   ├── layoutEngine.ts
│   │   └── validation.ts
│   ├── types/
│   │   ├── component.ts
│   │   ├── property.ts
│   │   ├── theme.ts
│   │   └── layout.ts
│   ├── App.tsx
│   └── main.tsx
├── package.json
├── vite.config.ts
├── tailwind.config.js
├── tsconfig.json
└── README.md
```

#### 2.2 State Management (Zustand)

```typescript
// designerStore.ts
import create from 'zustand';
import { devtools, persist } from 'zustand/middleware';

interface Component {
  id: string;
  type: string;
  properties: Record<string, any>;
  children: Component[];
  parent?: string;
}

interface DesignerState {
  // Canvas state
  components: Component[];
  selectedIds: string[];
  clipboard: Component[];

  // View state
  zoom: number;
  pan: { x: number; y: number };
  showGrid: boolean;

  // Theme state
  currentTheme: Theme;

  // Actions
  addComponent: (component: Component, parentId?: string) => void;
  removeComponent: (id: string) => void;
  updateComponent: (id: string, properties: Partial<Component>) => void;
  selectComponent: (id: string, multi?: boolean) => void;
  copyComponents: () => void;
  pasteComponents: () => void;
  undo: () => void;
  redo: () => void;
  setZoom: (zoom: number) => void;
  setPan: (pan: { x: number; y: number }) => void;
}

export const useDesignerStore = create<DesignerState>()(
  devtools(
    persist(
      (set, get) => ({
        // Initial state
        components: [],
        selectedIds: [],
        clipboard: [],
        zoom: 1.0,
        pan: { x: 0, y: 0 },
        showGrid: true,
        currentTheme: defaultTheme,

        // Actions
        addComponent: (component, parentId) => set((state) => {
          const newComponents = [...state.components];
          if (parentId) {
            const parent = findComponentById(newComponents, parentId);
            if (parent) {
              parent.children.push(component);
            }
          } else {
            newComponents.push(component);
          }
          return { components: newComponents };
        }),

        removeComponent: (id) => set((state) => ({
          components: removeComponentById(state.components, id),
          selectedIds: state.selectedIds.filter(sid => sid !== id)
        })),

        updateComponent: (id, properties) => set((state) => ({
          components: updateComponentById(state.components, id, properties)
        })),

        selectComponent: (id, multi = false) => set((state) => {
          if (multi) {
            return {
              selectedIds: state.selectedIds.includes(id)
                ? state.selectedIds.filter(sid => sid !== id)
                : [...state.selectedIds, id]
            };
          }
          return { selectedIds: [id] };
        }),

        copyComponents: () => set((state) => {
          const selected = state.selectedIds.map(id =>
            findComponentById(state.components, id)
          ).filter(Boolean) as Component[];
          return { clipboard: selected };
        }),

        pasteComponents: () => set((state) => {
          const pasted = state.clipboard.map(c => ({
            ...c,
            id: generateId(),
            properties: { ...c.properties }
          }));
          return { components: [...state.components, ...pasted] };
        }),

        // ... undo/redo implementation

        setZoom: (zoom) => set({ zoom }),
        setPan: (pan) => set({ pan }),
      }),
      {
        name: 'designer-storage',
        partialize: (state) => ({
          showGrid: state.showGrid,
          currentTheme: state.currentTheme
        })
      }
    )
  )
);
```

#### 2.3 API Client

```typescript
// PluginAPIClient.ts
class PluginAPIClient {
  private baseUrl: string;
  private ws: WebSocket | null = null;
  private listeners: Map<string, Set<Function>> = new Map();

  constructor(baseUrl: string = window.location.origin) {
    this.baseUrl = baseUrl;
    this.connectWebSocket();
  }

  // REST API methods
  async getComponents(): Promise<Component[]> {
    const res = await fetch(`${this.baseUrl}/api/components`);
    if (!res.ok) throw new Error('Failed to fetch components');
    return res.json();
  }

  async getComponent(id: string): Promise<Component> {
    const res = await fetch(`${this.baseUrl}/api/components/${id}`);
    if (!res.ok) throw new Error('Component not found');
    return res.json();
  }

  async createComponent(component: Omit<Component, 'id'>): Promise<Component> {
    const res = await fetch(`${this.baseUrl}/api/components`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(component)
    });
    if (!res.ok) throw new Error('Failed to create component');
    return res.json();
  }

  async updateComponent(id: string, updates: Partial<Component>): Promise<void> {
    const res = await fetch(`${this.baseUrl}/api/components/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(updates)
    });
    if (!res.ok) throw new Error('Failed to update component');
  }

  async deleteComponent(id: string): Promise<void> {
    const res = await fetch(`${this.baseUrl}/api/components/${id}`, {
      method: 'DELETE'
    });
    if (!res.ok) throw new Error('Failed to delete component');
  }

  // File operations
  async readFile(path: string): Promise<string> {
    const res = await fetch(`${this.baseUrl}/api/files/${path}`);
    if (!res.ok) throw new Error('File not found');
    return res.text();
  }

  async writeFile(path: string, content: string): Promise<void> {
    const res = await fetch(`${this.baseUrl}/api/files/${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain' },
      body: content
    });
    if (!res.ok) throw new Error('Failed to write file');
  }

  // WebSocket methods
  private connectWebSocket() {
    const wsUrl = this.baseUrl.replace(/^http/, 'ws') + '/ws';
    this.ws = new WebSocket(wsUrl);

    this.ws.onopen = () => {
      console.log('WebSocket connected');
      this.emit('connected', null);
    };

    this.ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      this.emit(message.type, message.data);
    };

    this.ws.onerror = (error) => {
      console.error('WebSocket error:', error);
      this.emit('error', error);
    };

    this.ws.onclose = () => {
      console.log('WebSocket disconnected, reconnecting...');
      setTimeout(() => this.connectWebSocket(), 1000);
    };
  }

  send(type: string, data: any) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({ type, data }));
    } else {
      console.warn('WebSocket not connected');
    }
  }

  on(event: string, callback: Function) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    this.listeners.get(event)!.add(callback);
  }

  off(event: string, callback: Function) {
    this.listeners.get(event)?.delete(callback);
  }

  private emit(event: string, data: any) {
    this.listeners.get(event)?.forEach(callback => callback(data));
  }
}

export const api = new PluginAPIClient();
```

#### 2.4 Drag-Drop Canvas

```typescript
// DropCanvas.tsx
import { useDndMonitor, DndContext, DragOverlay } from '@dnd-kit/core';
import { SortableContext, useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';

export function DropCanvas() {
  const { components, addComponent, updateComponent, selectComponent } = useDesignerStore();
  const [activeId, setActiveId] = useState<string | null>(null);

  const handleDragStart = (event: DragStartEvent) => {
    setActiveId(event.active.id as string);
  };

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;

    if (!over) {
      setActiveId(null);
      return;
    }

    // Check if dragging from palette (creating new component)
    if (active.data.current?.fromPalette) {
      const componentType = active.id as string;
      const newComponent = createComponent(componentType);

      // Add to parent if dropped over a container
      if (over.data.current?.isContainer) {
        addComponent(newComponent, over.id as string);
      } else {
        addComponent(newComponent);
      }
    } else {
      // Moving existing component
      const componentId = active.id as string;
      const newParentId = over.id as string;
      updateComponent(componentId, { parent: newParentId });
    }

    setActiveId(null);
  };

  return (
    <DndContext
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
      collisionDetection={closestCenter}
    >
      <div className="canvas-container">
        <GridOverlay />
        <ComponentTree components={components} />
        <SelectionOverlay />
        <ZoomControls />
      </div>

      <DragOverlay>
        {activeId ? <ComponentPreview id={activeId} /> : null}
      </DragOverlay>
    </DndContext>
  );
}

// ComponentTree.tsx
function ComponentTree({ components }: { components: Component[] }) {
  return (
    <div className="component-tree">
      {components.map(component => (
        <DraggableComponent key={component.id} component={component} />
      ))}
    </div>
  );
}

// DraggableComponent.tsx
function DraggableComponent({ component }: { component: Component }) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition
  } = useSortable({ id: component.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...attributes}
      {...listeners}
      className="draggable-component"
    >
      <ComponentRenderer component={component} />
      {component.children && (
        <ComponentTree components={component.children} />
      )}
    </div>
  );
}
```

---

## Communication Protocol

### REST API Specification

**Base URL:** `http://localhost:{random_port}/api`

**Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/components` | List all components |
| GET | `/components/{id}` | Get component by ID |
| POST | `/components` | Create new component |
| PUT | `/components/{id}` | Update component |
| DELETE | `/components/{id}` | Delete component |
| GET | `/files/{path}` | Read file content |
| POST | `/files/{path}` | Write file content |
| GET | `/project/info` | Get project metadata |

**Request/Response Examples:**

```json
// GET /components
{
  "components": [
    {
      "id": "btn-123",
      "type": "Button",
      "properties": {
        "text": "Click Me",
        "variant": "filled",
        "color": "primary"
      },
      "children": [],
      "parent": null
    }
  ]
}

// POST /components
// Request
{
  "type": "TextField",
  "properties": {
    "placeholder": "Enter name",
    "label": "Name"
  },
  "parent": "form-456"
}

// Response
{
  "id": "textfield-789",
  "type": "TextField",
  "properties": {
    "placeholder": "Enter name",
    "label": "Name"
  },
  "parent": "form-456"
}
```

### WebSocket Protocol

**URL:** `ws://localhost:{random_port}/ws`

**Message Format:**

```json
{
  "type": "message_type",
  "data": { ... }
}
```

**Message Types:**

| Type | Direction | Description |
|------|-----------|-------------|
| `component_created` | Server → Client | Component created in IDE |
| `component_updated` | Bidirectional | Component properties changed |
| `component_deleted` | Server → Client | Component deleted in IDE |
| `file_changed` | Server → Client | File modified externally |
| `file_reloaded` | Client → Server | Request file reload |
| `selection_changed` | Bidirectional | Selection changed |
| `ping` | Bidirectional | Keep-alive |

**Example Messages:**

```json
// Component updated (Client → Server)
{
  "type": "component_updated",
  "data": {
    "id": "btn-123",
    "properties": {
      "text": "New Text"
    }
  }
}

// File changed (Server → Client)
{
  "type": "file_changed",
  "data": {
    "path": "/src/screens/HomeScreen.ava",
    "content": "Screen { ... }"
  }
}
```

---

## Build & Deployment

### Build Process

#### Plugin Build (Gradle)

```kotlin
// build.gradle.kts
plugins {
    id("org.jetbrains.intellij") version "1.17.2"
    kotlin("jvm") version "1.9.25"
}

intellij {
    version.set("2023.2")
    type.set("IC") // IntelliJ IDEA Community
    plugins.set(listOf("java", "Kotlin"))
}

dependencies {
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-websockets:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
}

tasks {
    // Build React UI first
    register("buildReactUI") {
        doLast {
            exec {
                workingDir = file("web-ui")
                commandLine("npm", "run", "build")
            }
        }
    }

    // Copy React build to resources
    register<Copy>("copyReactBuild") {
        dependsOn("buildReactUI")
        from("web-ui/dist")
        into("src/main/resources/web-ui")
    }

    // Ensure React UI is built before plugin
    prepareSandbox {
        dependsOn("copyReactBuild")
    }

    patchPluginXml {
        version.set("0.2.0-beta")
        sinceBuild.set("232")
        untilBuild.set("241.*")
    }

    publishPlugin {
        token.set(System.getenv("JETBRAINS_TOKEN"))
    }
}
```

#### React UI Build (Vite)

```typescript
// vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  base: './', // Relative paths for embedded use
  build: {
    outDir: 'dist',
    sourcemap: false, // Disable for production
    rollupOptions: {
      output: {
        manualChunks: {
          'react-vendor': ['react', 'react-dom'],
          'dnd-kit': ['@dnd-kit/core', '@dnd-kit/sortable'],
        }
      }
    }
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  }
});
```

### Deployment Process

1. **Build React UI:** `cd web-ui && npm run build`
2. **Copy to plugin resources:** `./gradlew copyReactBuild`
3. **Build plugin:** `./gradlew buildPlugin`
4. **Test locally:** `./gradlew runIde`
5. **Publish to marketplace:** `./gradlew publishPlugin`

---

## Performance Optimization

### Target Metrics

| Metric | Target | Critical |
|--------|--------|----------|
| Canvas FPS | 60 FPS | >30 FPS |
| Component render time | <16ms | <50ms |
| Memory usage | <250MB | <500MB |
| Initial load time | <2s | <5s |
| WebSocket latency | <50ms | <200ms |

### Optimization Strategies

1. **Virtual Scrolling:** Render only visible components
2. **Lazy Loading:** Load component definitions on-demand
3. **Debouncing:** Debounce property updates (300ms)
4. **Memoization:** Use React.memo for expensive components
5. **Canvas Optimization:** Use CSS transforms instead of absolute positioning
6. **Bundle Splitting:** Code-split React app by route/feature

---

## Testing Strategy

### Unit Tests (Kotlin)

```kotlin
class EmbeddedWebServerTest {
    @Test
    fun `should start server on available port`() {
        val server = EmbeddedWebServer()
        val port = server.start()

        assertTrue(port > 0)
        assertTrue(isPortOpen(port))

        server.stop()
    }

    @Test
    fun `should serve static React app`() = runBlocking {
        val server = EmbeddedWebServer()
        val port = server.start()

        val response = httpClient.get("http://localhost:$port/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("<!DOCTYPE html>"))

        server.stop()
    }
}
```

### Integration Tests (TypeScript)

```typescript
describe('PluginAPIClient', () => {
  let api: PluginAPIClient;

  beforeEach(() => {
    api = new PluginAPIClient('http://localhost:8080');
  });

  it('should fetch components', async () => {
    const components = await api.getComponents();
    expect(components).toBeInstanceOf(Array);
  });

  it('should create component', async () => {
    const component = await api.createComponent({
      type: 'Button',
      properties: { text: 'Test' },
      children: []
    });

    expect(component.id).toBeDefined();
    expect(component.type).toBe('Button');
  });
});
```

### E2E Tests (Playwright)

```typescript
test('should drag component from palette to canvas', async ({ page }) => {
  await page.goto('http://localhost:8080');

  // Drag Button from palette
  const button = page.locator('[data-component-type="Button"]');
  const canvas = page.locator('.drop-canvas');

  await button.dragTo(canvas);

  // Verify component appears on canvas
  const addedComponent = page.locator('.canvas-component[data-type="Button"]');
  await expect(addedComponent).toBeVisible();
});
```

---

## Security Considerations

### Threats & Mitigations

| Threat | Mitigation |
|--------|------------|
| XSS in web UI | Sanitize all user input, use React's built-in escaping |
| CSRF | Serve only on localhost, no external access |
| Code injection | Validate DSL syntax before execution |
| Unauthorized file access | Restrict file operations to project directory |
| WebSocket hijacking | Localhost-only, no authentication needed |

### Best Practices

1. **Localhost-only:** Never expose web server to external network
2. **Input validation:** Validate all API inputs
3. **Content Security Policy:** Strict CSP headers
4. **HTTPS (optional):** Use HTTPS for paranoid users (self-signed cert)
5. **Sandboxing:** Run code generators in isolated process

---

## Conclusion

The Hybrid Architecture provides the best foundation for AVAMagic Studio:

- **Deep IDE integration** via IntelliJ Platform SDK
- **Modern visual designer** via React web UI
- **Reusability** for VS Code, standalone, and SaaS
- **Performance** with targeted optimizations
- **Scalability** to handle large projects

This architecture positions AVAMagic Studio as the most advanced multi-platform UI development tool in the industry.

---

**Document Version:** 1.0.0
**Last Updated:** 2025-11-21
**Next Review:** After v0.2.0-beta implementation
**Authors:** Manoj Jhawar, IDEACODE 8.4 Framework
**License:** Proprietary - Avanues Project

---

**End of Document**
