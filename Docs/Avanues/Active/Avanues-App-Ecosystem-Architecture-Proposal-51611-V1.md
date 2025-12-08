# Avanues App Ecosystem Architecture Proposal
**Date**: October 28, 2025
**Status**: PROPOSAL - Pending Approval
**Author**: Claude Code Analysis

---

## EXECUTIVE SUMMARY

This document proposes a **highly viable App Store-compliant architecture** for the Avanues ecosystem that addresses:

1. **User's Vision**: Separately compiled apps (not plugins) that share compiled capabilities
2. **App Store Compliance**: DSL/YAML micro-apps that don't trigger dynamic code execution restrictions
3. **Zero Bloat**: Core app remains lean (~30MB), feature apps downloaded on-demand
4. **Capability Discovery**: Apps advertise their compiled components via manifest files
5. **Asset Remixing**: Users create micro-apps in Avanues that use capabilities from all installed apps

**KEY INSIGHT**: By moving from "dynamic plugin loading" to "inter-app capability sharing via manifests + IPC", we avoid App Store restrictions while achieving the user's vision.

---

## 1. THE ARCHITECTURE

### 1.1 Core Concept

```
┌─────────────────────────────────────────────────────────────────┐
│              Avanues (Core App) - 30MB                       │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  AvaCode (DSL Parser + Code Generator)                 │   │
│  │  AvaUI (Component Runtime + Registry)                  │   │
│  │  VoiceOS (Voice Command System)                          │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Capability Discovery Engine (NEW)                       │   │
│  │  - Scans installed apps for .voiceapp manifests         │   │
│  │  - Reads capability declarations                         │   │
│  │  - Maintains registry of available compiled components  │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Micro-App Runtime (NEW)                                 │   │
│  │  - Interprets user-created .vos files                   │   │
│  │  - Dispatches to compiled apps via IPC when needed     │   │
│  │  - No dynamic code loading (App Store compliant!)       │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
         │                           │                      │
         │ IPC                       │ IPC                  │ IPC
         ▼                           ▼                      ▼
┌─────────────────┐    ┌──────────────────────┐    ┌──────────────┐
│  AI App (50MB)  │    │ Browser App (40MB)   │    │ Notes (20MB) │
│ ┌─────────────┐ │    │ ┌──────────────────┐ │    │ ┌──────────┐ │
│ │.voiceapp    │ │    │ │.voiceapp         │ │    │ │.voiceapp │ │
│ │manifest     │ │    │ │manifest          │ │    │ │manifest  │ │
│ │             │ │    │ │                  │ │    │ │          │ │
│ │capabilities:│ │    │ │capabilities:     │ │    │ │caps:     │ │
│ │- llm.chat   │ │    │ │- browser.render  │ │    │ │- notes   │ │
│ │- nlp.entity │ │    │ │- browser.search  │ │    │ │- markdown│ │
│ └─────────────┘ │    │ └──────────────────┘ │    │ └──────────┘ │
│  Compiled Code  │    │   Compiled Code      │    │ Compiled Code│
└─────────────────┘    └──────────────────────┘    └──────────────┘
```

### 1.2 How It Works

**Scenario**: User creates a "Smart Note Taker" micro-app

1. **User writes DSL in Avanues**:
```yaml
# smart-note-taker.vos (DSL file, NOT code!)
App {
    id: "com.user.smartnotes"
    name: "Smart Note Taker"

    Container {
        TextField {
            id: "noteInput"
            placeholder: "Write your note..."
        }

        Button {
            id: "analyzeBtn"
            label: "Analyze Sentiment"
            onClick: => {
                # Call AI app's capability
                result = AIApp.analyzeSentiment(noteInput.text)
                sentimentLabel.text = result.label
            }
        }

        Button {
            id: "saveBtn"
            label: "Save Note"
            onClick: => {
                # Call Notes app's capability
                NotesApp.saveNote(noteInput.text)
            }
        }

        Text {
            id: "sentimentLabel"
            content: "Sentiment: Unknown"
        }
    }
}
```

2. **Avanues discovers capabilities** (on app startup):
```kotlin
// Capability Discovery Engine
class AppCapabilityScanner {
    suspend fun scanInstalledApps(): Map<String, AppCapabilities> {
        val capabilities = mutableMapOf<String, AppCapabilities>()

        // On Android: Query PackageManager for apps with .voiceapp manifest
        val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)

        for (pkg in packages) {
            if (pkg.hasVoiceAppManifest()) {
                val manifest = readVoiceAppManifest(pkg.packageName)
                capabilities[pkg.packageName] = manifest.capabilities
            }
        }

        return capabilities
    }
}
```

3. **User runs the micro-app**:
   - Avanues **interprets** the DSL (no code generation needed at runtime!)
   - When `AIApp.analyzeSentiment()` is called, Avanues:
     - Checks capability registry: Is AI App installed?
     - If YES: Sends IPC message to AI App with text
     - AI App processes request, returns result
     - Avanues updates UI
   - If NO: Shows popup "AI App required. Download from App Store?"

4. **No code is loaded dynamically** - only data flows between apps via IPC!

---

## 2. APP STORE COMPLIANCE ANALYSIS

### 2.1 Apple App Store

**Relevant Guidelines**:
- **2.5.2**: "Apps may not download, install, or execute code"
- **2.5.6**: "Apps that browse the web must use WebKit"
- **Allowed**: "Interpreted code" (JavaScript, Lua, Python in sandboxed interpreter)

**Our Approach**:

| Feature | Compliant? | Reasoning |
|---------|-----------|-----------|
| User creates .vos DSL files | ✅ YES | **Data files, not code**. Similar to shortcuts, automations, or JSON configs |
| Avanues interprets DSL | ✅ YES | Interpretation engine is **pre-compiled in the app**. No new code downloaded |
| IPC calls to other apps | ✅ YES | Standard iOS inter-app communication (URL schemes, Universal Links, App Extensions) |
| Apps share capabilities | ✅ YES | **Metadata exchange only**. No code sharing |
| Micro-apps saved locally | ✅ YES | User-generated data files (like Shortcuts app) |

**Precedents** (Apps that do similar things):
1. **Shortcuts** - Users create workflows that call other apps ✅ Approved
2. **IFTTT** - Users create automation rules ✅ Approved
3. **Scriptable** - Users write JavaScript that calls iOS APIs ✅ Approved
4. **Pythonista** - Python interpreter with sandboxed code execution ✅ Approved

**Risk Level**: **LOW** - As long as:
- .vos files are treated as **data** (like JSON, YAML)
- No `eval()` or dynamic code compilation
- Interpretation engine is pre-compiled
- IPC uses standard iOS mechanisms (URL schemes, App Extensions, Shared Containers)

### 2.2 Google Play Store

**Relevant Policies**:
- **Software Principles**: "Apps that introduce or exploit security vulnerabilities will be removed"
- **DGG (Developer Program Policies)**: Code execution is allowed as long as:
  - Not used for circumventing security
  - User understands what code does
  - Code doesn't violate policies

**Our Approach**:

| Feature | Compliant? | Reasoning |
|---------|-----------|-----------|
| User creates .vos DSL files | ✅ YES | User-generated content. Similar to Tasker, Automate, MacroDroid |
| Avanues interprets DSL | ✅ YES | Pre-approved interpretation logic. No arbitrary code execution |
| IPC calls to other apps | ✅ YES | Standard Android Intents, Services, ContentProviders |
| Apps share capabilities | ✅ YES | Metadata only |

**Precedents**:
1. **Tasker** - Users create automation scripts ✅ Approved
2. **Automate** - Visual programming for Android ✅ Approved
3. **Termux** - Full terminal with script execution ✅ Approved
4. **AIDE** - IDE that compiles Android apps on-device ✅ Approved

**Risk Level**: **VERY LOW** - Google Play is much more permissive than Apple. As long as:
- User explicitly creates the micro-apps
- No security vulnerabilities exploited
- Standard IPC mechanisms used

---

## 3. CAPABILITY MANIFEST SPECIFICATION

### 3.1 .voiceapp Manifest Format

Each compiled app (AI, Browser, Notes) includes a `.voiceapp` manifest:

**File**: `AIApp/assets/capabilities.voiceapp` (YAML format)

```yaml
# .voiceapp Manifest v1.0
app:
  packageName: com.avanues.ai
  displayName: Avanues AI
  version: 1.0.0
  publisher: Augmentalis Inc.
  icon: "app://icon.png"

# Capabilities this app provides
capabilities:
  # LLM capabilities
  - id: "llm.chat"
    displayName: "AI Chat"
    description: "Multi-turn conversational AI"
    inputSchema:
      - name: "messages"
        type: "array<string>"
        required: true
      - name: "model"
        type: "enum"
        values: ["gpt4", "claude", "llama"]
        default: "gpt4"
    outputSchema:
      type: "string"
    rateLimit:
      perMinute: 10
      perDay: 1000
    requiresAuth: false

  - id: "nlp.sentiment"
    displayName: "Sentiment Analysis"
    description: "Analyze emotional tone of text"
    inputSchema:
      - name: "text"
        type: "string"
        required: true
    outputSchema:
      label: "string"    # positive, negative, neutral
      score: "float"     # -1.0 to 1.0
      confidence: "float" # 0.0 to 1.0
    rateLimit:
      perMinute: 100

  - id: "nlp.entities"
    displayName: "Entity Extraction"
    description: "Extract persons, places, organizations"
    inputSchema:
      - name: "text"
        type: "string"
        required: true
    outputSchema:
      type: "array<entity>"
      entity:
        text: "string"
        type: "enum"  # PERSON, LOCATION, ORGANIZATION
        startIndex: "int"
        endIndex: "int"

# AvaUI Components this app provides
components:
  - type: "AIChat"
    displayName: "AI Chat Component"
    category: "ai"
    properties:
      - name: "model"
        type: "string"
        default: "gpt4"
      - name: "systemPrompt"
        type: "string"
        default: ""
    callbacks:
      - name: "onResponse"
        parameters: ["response: string"]

# IPC Configuration
ipc:
  protocol: "intent"  # Android
  service: "com.avanues.ai.CapabilityService"
  urlScheme: "voiceai://"  # iOS

# Requirements
requirements:
  minAvanuesVersion: "1.0.0"
  permissions:
    - NETWORK
    - STORAGE_READ
  minAndroidVersion: 26
  minIOSVersion: "14.0"
```

### 3.2 Capability Discovery API

**In Avanues Core**:

```kotlin
// CapabilityDiscoveryEngine.kt
class CapabilityDiscoveryEngine(
    private val context: Context,
    private val packageManager: PackageManager
) {
    private val capabilityRegistry = mutableMapOf<String, AppCapabilities>()

    /**
     * Scan all installed apps for .voiceapp manifests
     */
    suspend fun discoverCapabilities(): CapabilityRegistry {
        capabilityRegistry.clear()

        // Query all installed apps
        val packages = packageManager.getInstalledPackages(
            PackageManager.GET_META_DATA
        )

        for (pkg in packages) {
            try {
                // Check if app has .voiceapp manifest in assets
                val manifest = readVoiceAppManifest(pkg.packageName)
                if (manifest != null) {
                    capabilityRegistry[pkg.packageName] = manifest
                    println("✓ Discovered capabilities from ${pkg.packageName}")
                }
            } catch (e: Exception) {
                // App doesn't have .voiceapp manifest, skip
            }
        }

        return CapabilityRegistry(capabilityRegistry)
    }

    /**
     * Read .voiceapp manifest from app's assets
     */
    private fun readVoiceAppManifest(packageName: String): AppCapabilities? {
        try {
            val appContext = context.createPackageContext(
                packageName,
                Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
            )

            val inputStream = appContext.assets.open("capabilities.voiceapp")
            val yamlContent = inputStream.bufferedReader().use { it.readText() }

            return Yaml.decodeFromString<AppCapabilities>(yamlContent)
        } catch (e: Exception) {
            return null
        }
    }
}

data class AppCapabilities(
    val app: AppMetadata,
    val capabilities: List<CapabilityDescriptor>,
    val components: List<ComponentDescriptor>,
    val ipc: IPCConfiguration,
    val requirements: AppRequirements
)

data class CapabilityDescriptor(
    val id: String,               // "nlp.sentiment"
    val displayName: String,      // "Sentiment Analysis"
    val description: String,
    val inputSchema: List<Parameter>,
    val outputSchema: Any,
    val rateLimit: RateLimit?,
    val requiresAuth: Boolean
)
```

---

## 4. IPC CAPABILITY INVOCATION

### 4.1 Capability Call Flow

**User DSL**:
```yaml
Button {
    onClick: => {
        result = AIApp.analyzeSentiment(noteInput.text)
        sentimentLabel.text = result.label
    }
}
```

**Avanues Runtime Execution**:

```kotlin
// MicroAppRuntime.kt
class MicroAppRuntime(
    private val capabilityRegistry: CapabilityRegistry,
    private val ipcBridge: IPCBridge
) {

    suspend fun executeCallback(callbackName: String, context: CallbackContext) {
        // Parse DSL callback
        val ast = parseCallback(callbackName)

        // Execute each statement
        for (statement in ast.statements) {
            when (statement) {
                is CapabilityCall -> {
                    // AIApp.analyzeSentiment(noteInput.text)
                    val result = invokeCapability(
                        appName = statement.appName,       // "AIApp"
                        capabilityId = statement.capabilityId, // "nlp.sentiment"
                        params = statement.params          // {"text": "..."}
                    )
                    context.setVariable(statement.resultVar, result)
                }
                is Assignment -> {
                    // sentimentLabel.text = result.label
                    val component = context.getComponent(statement.componentId)
                    component.setProperty(statement.property, statement.value)
                }
            }
        }
    }

    /**
     * Invoke capability in external app via IPC
     */
    suspend fun invokeCapability(
        appName: String,
        capabilityId: String,
        params: Map<String, Any>
    ): CapabilityResult {
        // 1. Resolve app package name
        val packageName = resolveAppPackage(appName)
        if (packageName == null) {
            throw AppNotInstalledException(appName)
        }

        // 2. Get capability descriptor
        val capability = capabilityRegistry.getCapability(packageName, capabilityId)
        if (capability == null) {
            throw CapabilityNotFoundException(capabilityId)
        }

        // 3. Validate input parameters
        validateParams(params, capability.inputSchema)

        // 4. Send IPC request to app
        return ipcBridge.invokeCapability(
            packageName = packageName,
            capabilityId = capabilityId,
            params = params,
            timeout = 30_000  // 30 seconds
        )
    }

    private fun resolveAppPackage(appName: String): String? {
        // Map friendly name to package
        return when (appName) {
            "AIApp" -> "com.avanues.ai"
            "BrowserApp" -> "com.avanues.browser"
            "NotesApp" -> "com.avanues.notes"
            else -> capabilityRegistry.findAppByName(appName)
        }
    }
}
```

### 4.2 IPC Bridge Implementation

**Android (Intents + AIDL)**:

```kotlin
// IPCBridge.kt (Android)
class IPCBridge(private val context: Context) {

    suspend fun invokeCapability(
        packageName: String,
        capabilityId: String,
        params: Map<String, Any>,
        timeout: Long
    ): CapabilityResult = withContext(Dispatchers.IO) {

        // Create Intent for capability service
        val intent = Intent().apply {
            component = ComponentName(
                packageName,
                "$packageName.CapabilityService"
            )
            action = "com.avanues.INVOKE_CAPABILITY"
            putExtra("capabilityId", capabilityId)
            putExtra("params", Bundle().apply {
                params.forEach { (k, v) -> putString(k, v.toString()) }
            })
        }

        // Bind to service
        val serviceConnection = CompletableDeferred<CapabilityResult>()

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val capabilityService = ICapabilityService.Stub.asInterface(service)

                try {
                    val resultBundle = capabilityService.invokeCapability(
                        capabilityId,
                        Bundle().apply { params.forEach { (k, v) -> putString(k, v.toString()) } }
                    )
                    serviceConnection.complete(CapabilityResult.fromBundle(resultBundle))
                } catch (e: Exception) {
                    serviceConnection.completeExceptionally(e)
                } finally {
                    context.unbindService(this)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                serviceConnection.completeExceptionally(
                    ServiceDisconnectedException(packageName)
                )
            }
        }

        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        // Wait for result with timeout
        withTimeout(timeout) {
            serviceConnection.await()
        }
    }
}
```

**iOS (URL Schemes + App Extensions)**:

```swift
// IPCBridge.swift (iOS)
class IPCBridge {

    func invokeCapability(
        appName: String,
        capabilityId: String,
        params: [String: Any],
        timeout: TimeInterval = 30.0
    ) async throws -> CapabilityResult {

        // Construct URL scheme call
        let urlScheme = "\(appName.lowercased())://capability"
        var components = URLComponents(string: urlScheme)!

        components.queryItems = [
            URLQueryItem(name: "id", value: capabilityId),
            URLQueryItem(name: "params", value: encodeParams(params))
        ]

        guard let url = components.url else {
            throw IPCError.invalidURL
        }

        // Check if app is installed
        guard UIApplication.shared.canOpenURL(url) else {
            throw AppNotInstalledException(appName)
        }

        // Open URL and wait for callback
        return try await withCheckedThrowingContinuation { continuation in
            // Register callback handler
            registerCallback(capabilityId: capabilityId) { result in
                continuation.resume(returning: result)
            }

            // Open URL
            UIApplication.shared.open(url) { success in
                if !success {
                    continuation.resume(throwing: IPCError.failedToOpen)
                }
            }

            // Timeout
            DispatchQueue.main.asyncAfter(deadline: .now() + timeout) {
                continuation.resume(throwing: IPCError.timeout)
            }
        }
    }
}
```

---

## 5. EXAMPLE: AI APP IMPLEMENTATION

### 5.1 AI App Structure

```
AvanuesAI/
├── AndroidManifest.xml
├── assets/
│   └── capabilities.voiceapp        # Manifest declaring capabilities
├── src/
│   ├── AICapabilityService.kt        # IPC service endpoint
│   ├── SentimentAnalyzer.kt          # Compiled sentiment analysis
│   ├── EntityExtractor.kt            # Compiled NER
│   └── ChatEngine.kt                 # Compiled LLM interface
└── build.gradle.kts
```

### 5.2 AI App Capability Service

```kotlin
// AICapabilityService.kt
class AICapabilityService : Service() {

    private val binder = CapabilityServiceBinder()

    override fun onBind(intent: Intent?): IBinder? = binder

    inner class CapabilityServiceBinder : ICapabilityService.Stub() {

        override fun invokeCapability(
            capabilityId: String,
            params: Bundle
        ): Bundle {
            return when (capabilityId) {
                "nlp.sentiment" -> {
                    val text = params.getString("text") ?: ""
                    val result = SentimentAnalyzer.analyze(text)
                    Bundle().apply {
                        putString("label", result.label)
                        putDouble("score", result.score)
                        putDouble("confidence", result.confidence)
                    }
                }

                "nlp.entities" -> {
                    val text = params.getString("text") ?: ""
                    val entities = EntityExtractor.extract(text)
                    Bundle().apply {
                        putParcelableArrayList("entities", ArrayList(entities))
                    }
                }

                "llm.chat" -> {
                    val messages = params.getStringArrayList("messages") ?: emptyList()
                    val model = params.getString("model") ?: "gpt4"
                    val response = ChatEngine.chat(messages, model)
                    Bundle().apply {
                        putString("response", response)
                    }
                }

                else -> {
                    throw IllegalArgumentException("Unknown capability: $capabilityId")
                }
            }
        }

        override fun getCapabilities(): List<String> {
            return listOf("nlp.sentiment", "nlp.entities", "llm.chat")
        }
    }
}
```

### 5.3 AI App Manifest (Android)

```xml
<!-- AndroidManifest.xml -->
<manifest package="com.avanues.ai">
    <application>
        <!-- IPC Service -->
        <service
            android:name=".AICapabilityService"
            android:exported="true"
            android:permission="com.avanues.CAPABILITY_INVOKE">
            <intent-filter>
                <action android:name="com.avanues.INVOKE_CAPABILITY" />
            </intent-filter>

            <!-- Declare capabilities in metadata -->
            <meta-data
                android:name="voiceapp.capabilities"
                android:resource="@raw/capabilities" />
        </service>
    </application>
</manifest>
```

---

## 6. USER EXPERIENCE FLOW

### 6.1 Scenario: Creating a Smart Note Taker

**Step 1**: User opens Avanues Core app

**Step 2**: User creates new micro-app
- Tap "Create New App"
- Choose template or start from scratch
- Write DSL in visual editor or text editor

**Step 3**: User adds TextField + Button
```yaml
TextField {
    id: "noteInput"
}

Button {
    label: "Analyze Sentiment"
    onClick: => {
        result = AIApp.analyzeSentiment(noteInput.text)
    }
}
```

**Step 4**: Avanues checks capability availability
- Parses DSL
- Sees `AIApp.analyzeSentiment` call
- Checks capability registry: Is AI App installed?

**Step 5a**: If AI App is installed ✅
- Shows: "✓ All required apps available"
- User can test the micro-app immediately

**Step 5b**: If AI App is NOT installed ❌
- Shows: "⚠️ Required Apps Missing:"
  - AI App (for sentiment analysis)
  - [Download from App Store]
- User taps download, installs AI App
- Returns to Avanues, tries again ✅

**Step 6**: User runs micro-app
- Types note in TextField
- Taps "Analyze Sentiment" button
- Avanues sends IPC request to AI App
- AI App processes text, returns result
- Avanues displays sentiment in UI

**Step 7**: User shares micro-app
- Exports .vos file (just DSL text!)
- Shares via AirDrop, messaging, etc.
- Other users can import and run (if they have required apps)

---

## 7. APP STORE COMPLIANCE SUMMARY

### 7.1 Why This Architecture is Compliant

| Concern | Traditional Plugin System | Our Approach | Compliant? |
|---------|-------------------------|--------------|-----------|
| **Dynamic Code Loading** | Loads .jar/.dex files at runtime | No code loading - only DSL interpretation | ✅ YES |
| **Code Execution** | Executes plugin bytecode | Interprets DSL (data) + calls pre-compiled apps via IPC | ✅ YES |
| **Sandboxing** | Plugins run in same process | Each app runs in separate process | ✅ YES |
| **User Control** | User doesn't create plugins | **User creates DSL micro-apps** (like Shortcuts) | ✅ YES |
| **Distribution** | Sideloading required | Apps distributed via App Store | ✅ YES |
| **Review Process** | Each plugin needs review | Each app reviewed once, micro-apps are user data | ✅ YES |

### 7.2 Key Compliance Points

**Apple App Store** ✅:
1. .vos files are **user-generated data** (like Shortcuts, IFTTT)
2. Interpretation engine is **pre-compiled** in Avanues
3. IPC uses **standard iOS mechanisms** (URL schemes, App Extensions)
4. No `eval()`, no dynamic compilation
5. Each app is **independently reviewed**

**Google Play Store** ✅:
1. Similar to Tasker, Automate (both approved)
2. User explicitly creates automation rules
3. Standard Android IPC (Intents, Services)
4. No security exploits
5. Transparent to user

### 7.3 Precedent Analysis

**Shortcuts (Apple)** - APPROVED ✅
- Users create workflows that call other apps
- Uses "actions" provided by installed apps
- **Exact same model as our capability system**

**IFTTT (Apple + Google)** - APPROVED ✅
- Users create automation rules
- Calls services provided by other apps
- **Data-driven rule evaluation**

**Tasker (Google)** - APPROVED ✅
- Users write automation scripts
- Calls Android intents/services
- **More powerful than our DSL, still approved**

**Scriptable (Apple)** - APPROVED ✅
- Users write JavaScript code
- JavaScript interpreter is pre-compiled
- **Actual code execution, still approved because user writes it**

---

## 8. IMPLEMENTATION ROADMAP

### Phase 1: Core Infrastructure (Weeks 1-2)
- [ ] Design .voiceapp manifest schema
- [ ] Implement CapabilityDiscoveryEngine
- [ ] Create CapabilityRegistry
- [ ] Build IPCBridge for Android (Intents + AIDL)
- [ ] Build IPCBridge for iOS (URL schemes)

### Phase 2: Example Apps (Weeks 3-4)
- [ ] Create AI App template with CapabilityService
- [ ] Implement 3 sample capabilities (sentiment, entities, chat)
- [ ] Create .voiceapp manifest for AI App
- [ ] Test IPC calls from Avanues to AI App

### Phase 3: MicroApp Runtime (Weeks 5-6)
- [ ] Extend VoiceOS DSL parser for capability calls
- [ ] Implement MicroAppRuntime interpreter
- [ ] Add capability call execution
- [ ] Handle missing app scenarios (prompts)

### Phase 4: User Experience (Weeks 7-8)
- [ ] Build micro-app editor UI
- [ ] Add capability autocomplete (based on installed apps)
- [ ] Implement "missing app" detection + download prompts
- [ ] Add micro-app sharing (.vos export/import)

### Phase 5: Additional Apps (Weeks 9-12)
- [ ] Browser App (browser.render, browser.search)
- [ ] Notes App (notes.save, notes.load, notes.search)
- [ ] Forms App (forms.create, forms.validate)
- [ ] Create .voiceapp manifests for each

### Phase 6: App Store Submission (Week 13-14)
- [ ] Prepare documentation for Apple review
- [ ] Prepare documentation for Google review
- [ ] Submit Avanues Core app
- [ ] Submit AI, Browser, Notes apps
- [ ] Address review feedback

---

## 9. TECHNICAL ADVANTAGES

### 9.1 Zero Bloat
- **Avanues Core**: 30MB (AvaCode + AvaUI + Discovery Engine)
- **AI App**: 50MB (only if user needs AI features)
- **Browser App**: 40MB (only if user needs browser)
- **Notes App**: 20MB (only if user needs notes)
- **User installs only what they need**

### 9.2 Process Isolation
- Each app runs in separate process
- Crash in AI App doesn't affect Avanues
- Memory limits per-app (not shared)
- Better security (sandboxed)

### 9.3 Independent Updates
- AI App can update without Avanues update
- New capabilities can be added without core app changes
- Faster iteration cycle

### 9.4 Monetization Options
- Core app: Free (AvaCode + AvaUI)
- AI App: $9.99 (or subscription for cloud API access)
- Browser App: $4.99
- Notes App: Free with Pro upgrade
- Users pay only for features they use

### 9.5 Developer Ecosystem
- Third-party developers can create capability apps
- Publish to App Store independently
- Include .voiceapp manifest
- Users can use any app that provides capabilities

---

## 10. RISK MITIGATION

### Risk 1: Apple Rejects for "Code Execution"
**Mitigation**:
- Emphasize DSL is **user-generated data**
- Reference Shortcuts, IFTTT precedents
- Provide detailed technical documentation
- Offer to work with Apple review team

**Probability**: LOW (10%)

### Risk 2: IPC Performance Too Slow
**Mitigation**:
- Benchmark IPC calls (target <50ms per call)
- Implement capability batching for multi-step operations
- Cache results where appropriate
- Use async/await patterns

**Probability**: MEDIUM (30%)

### Risk 3: Users Confused by "Missing App" Prompts
**Mitigation**:
- Clear onboarding explaining app ecosystem
- Visual indicators in DSL editor showing available capabilities
- One-tap install from prompts
- Template micro-apps that work with common app combinations

**Probability**: MEDIUM (40%)

### Risk 4: Capability Versioning Conflicts
**Mitigation**:
- Include version in .voiceapp manifest
- MicroApp declares minimum capability version required
- Avanues warns if version mismatch
- Graceful fallback or update prompt

**Probability**: MEDIUM (35%)

---

## 11. CONCLUSION

### Is This Architecture Viable?

**YES - HIGHLY VIABLE** ✅

**Reasons**:
1. **App Store Compliant**: No dynamic code loading, only DSL interpretation + IPC
2. **Zero Bloat**: Core app ~30MB, features downloaded on-demand
3. **User-Friendly**: Similar UX to Shortcuts, IFTTT (proven successful)
4. **Technically Sound**: Leverages existing AvaCode/AvaUI, adds capability discovery + IPC
5. **Precedents Exist**: Shortcuts, IFTTT, Tasker all use similar models and are approved

**Key Insight**: By treating micro-apps as **user-generated data** rather than **code**, we comply with store policies while achieving the desired functionality.

### Next Steps

1. **Validate with stakeholders**: Review this proposal
2. **Prototype Phase 1**: Build capability discovery + IPC bridge
3. **Create proof-of-concept**: One micro-app calling AI App capability
4. **Test App Store submission**: Submit simple version for early feedback
5. **Iterate based on feedback**: Adjust architecture if needed

---

## 12. APPENDIX: COMPARISON MATRIX

| Feature | Traditional Plugin System | Our Capability System | Winner |
|---------|---------------------------|---------------------|--------|
| **App Size** | 200MB+ (all plugins bundled) | 30MB core + optional apps | Capability ✅ |
| **App Store Compliance** | ❌ iOS rejects dynamic loading | ✅ DSL interpretation only | Capability ✅ |
| **Process Isolation** | ❌ Same process, shared heap | ✅ Separate processes | Capability ✅ |
| **Developer Distribution** | Sideloading only | App Store distribution | Capability ✅ |
| **User Control** | Developer creates plugins | **User creates micro-apps** | Capability ✅ |
| **Monetization** | Bundle pricing only | Per-app pricing | Capability ✅ |
| **IPC Latency** | 0ms (in-process) | 10-50ms (IPC call) | Plugin ⚠️ |
| **Setup Complexity** | Simple (pre-bundled) | Requires multiple apps | Plugin ⚠️ |

**Overall Winner**: **Capability System** (8-2)

---

**End of Proposal**

**Status**: Ready for stakeholder review
**Author**: Claude Code Analysis
**Date**: October 28, 2025
