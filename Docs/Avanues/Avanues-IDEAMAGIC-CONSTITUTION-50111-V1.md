# IDEAMagic System Constitution
**Document Type:** Master Constitution (Living Document)
**Created:** 2025-11-01 14:12 PST
**Status:** AUTHORITATIVE - All implementations MUST comply
**Version:** 1.1.0
**Authority:** Manoj Jhawar, manoj@ideahq.net
**Supersedes:** MAGICIDEA-CONSTITUTION-251101-1232.md (v1.0.0)

---

## Preamble

This Constitution establishes the foundational principles, architectural decisions, and immutable rules for the **IDEAMagic System** - the complete intelligent development ecosystem.

**Vision**: Where magical simplicity meets intelligent architecture.

**Mission**: Enable developers to build voice-first, intelligent apps 10× faster with 96% less code.

**Core Philosophy**:
- **"Magic"** = Simplicity, ease-of-use, developer delight
- **"IDEA"** = Intelligent Devices Enhanced Architecture, professional credibility

---

## Article 0: Repository Strategy (Monorepo)

### Section 1: Monorepo Mandate

**CONSTITUTIONAL RULE #0**: IDEAMagic System SHALL remain in the Avanues monorepo. Separation into a separate repository is PROHIBITED unless this article is amended.

**Rationale**:
- **Industry Standard**: Microsoft, Google, Apple, Meta all use monorepos for their ecosystems
- **Atomic Commits**: Change IDEAMagic + update VoiceOS apps in same commit
- **Faster Iteration**: Single build, immediate testing across all apps
- **Shared Dependencies**: uuidcreator, themes, renderers all in one place
- **Simpler CI/CD**: One pipeline for entire ecosystem
- **Dogfooding**: VoiceOS/Avanue apps use IDEAMagic immediately (find bugs fast)

**Repository Structure**:
```
Avanues/ (Single Monorepo)
├── Universal/
│   └── IDEAMagic/                  # Umbrella folder
│       ├── AvaCode/              # DSL compiler
│       ├── AvaUI/                # UI framework
│       ├── IDEACode/               # AI assistant (future)
│       ├── IDEAFlow/               # Workflow engine (future)
│       └── VoiceUI/                # Voice SDK (future)
│
├── android/
│   └── standalone-libraries/
│       └── uuidcreator/            # Existing UUID system (leverage for VUID)
│
├── apps/
│   ├── voiceos/                    # Uses IDEAMagic
│   ├── avanuelaunch/               # Uses IDEAMagic
│   └── aiavanue/                   # Will use IDEAMagic
│
└── docs/
    └── MAGICIDEA-*                 # All IDEAMagic documentation
```

**Commit Strategy**:
```bash
# Use "magicidea:" prefix for easy categorization
git commit -m "magicidea(avacode): Add DSL parser"
git commit -m "magicidea(voiceui): Add VUID routing"
```

**ENFORCEMENT**: Any proposal to extract IDEAMagic to a separate repository MUST be rejected unless:
1. This constitutional article is formally amended (14-day discussion + board vote)
2. Compelling technical reason demonstrated (cannot be solved in monorepo)
3. Approved by Benevolent Dictator

**Real-World Examples**:
- **Microsoft**: Windows, Office, VS Code, Azure all in microsoft/microsoft-ui-xaml
- **Google**: Android, Chrome, YouTube all in google3 monorepo
- **Apple**: iOS, macOS, watchOS, tvOS all in single internal monorepo
- **Meta**: Facebook, Instagram, WhatsApp all in single monorepo

---

## Article I: IDEAMagic System Definition

### Section 1: System Composition

**CONSTITUTIONAL RULE #1**: The IDEAMagic System SHALL consist of five integrated components.

```
IDEAMagic System
├── AvaCode (DSL Language)
├── AvaUI (UI Framework)
├── IDEACode (AI Coding Assistant)
├── IDEAFlow (Workflow Engine)
└── VoiceUI SDK (Voice Integration)
```

**Package Namespace**: `com.augmentalis.magicidea.*`

**Unified Branding**: All components MUST use "IDEAMagic" umbrella branding.

---

### Section 2: Component Definitions

#### AvaCode (DSL Language)

**Definition**: The declarative domain-specific language for building UIs with minimal code.

**Package**: `com.augmentalis.magicidea.avacode`

**Purpose**:
- Provide the world's most concise UI language
- 96% less code than Unity, 87% less than Compose
- Compile-time code generation (0% runtime overhead)
- Multi-format support (DSL, YAML, JSON)

**Example**:
```kotlin
Btn("Click Me", w=200.dp, bg=Blue) { println("Clicked") }
```

**Status**: Foundation component (MUST exist for system to function)

---

#### AvaUI (UI Framework)

**Definition**: The runtime UI framework providing production-ready components and platform renderers.

**Package**: `com.augmentalis.magicidea.avaui`

**Purpose**:
- Provide 50 production-ready UI components
- Support 6+ platforms (Android, iOS, Desktop, Web, etc.)
- Deliver 7 platform-native themes
- Enable state management and theme customization

**Components**:
- Runtime: Btn, Txt, Field, V, H, etc. (50 total)
- Themes: iOS 26, Material 3, Windows 11, macOS 26, visionOS 2, Android XR, One UI 7
- Renderers: Android (Compose), iOS (SwiftUI), Desktop (Compose), Web (React)

**Status**: Foundation component (MUST exist for system to function)

---

#### IDEACode (AI Coding Assistant)

**Definition**: AI-powered development assistant using IDEACODE protocol for intelligent code generation.

**Package**: `com.augmentalis.magicidea.ideacode`

**Purpose**:
- Natural language → AvaCode DSL compilation
- AI code completion and review
- IDEACODE protocol integration (COT/TOT/Extended Thinking)
- Performance optimization suggestions

**Example**:
```
User: "Create a login form with email validation"
IDEACode: [Generates complete AvaCode DSL with validation logic]
```

**Status**: Enhancement component (Optional but recommended)

---

#### IDEAFlow (Workflow Engine)

**Definition**: Visual workflow builder and automation engine for multi-plugin orchestration.

**Package**: `com.augmentalis.magicidea.ideaflow`

**Purpose**:
- Enable visual workflow creation (drag-and-drop)
- Provide 200+ pre-built actions
- Support multi-plugin orchestration via capability system
- Enable voice-triggered workflows

**Example**:
```yaml
trigger: "Every morning at 8 AM"
actions:
  - getWeather()
  - getCalendarEvents()
  - generateSummary()
  - speakResult()
```

**Status**: Enhancement component (Optional)

---

#### VoiceUI SDK (Voice Integration)

**Definition**: Universal voice integration SDK enabling voice-first development and cross-app voice control.

**Package**: `com.augmentalis.magicidea.voiceui`

**Purpose**:
- Universal Voice UUID (VUID) system for global voice routing
- Voice command registration and handling
- Speech recognition and synthesis
- Cross-app voice workflows
- Accessibility enhancements (WCAG 2.1 AAA)

**Example**:
```kotlin
Btn("Submit").voiceCommand("submit form")

@VoiceAction(vuid = "weather.current")
suspend fun speakWeather() {
    speak("It's ${temp} degrees")
}
```

**Status**: Differentiator component (Killer feature, monetization driver)

---

### Section 3: Unified Package Mandate

**CONSTITUTIONAL RULE #2**: AvaCode and AvaUI SHALL be distributed as a single unified package for end-users.

**Rationale**:
- Inseparable dependencies (AvaCode generates AvaUI, AvaUI consumes AvaCode)
- Version synchronization critical
- User simplicity (one dependency, not multiple)
- Industry standard (React, Flutter, Compose all unified)

**User Dependency** (Simple):
```kotlin
// Single package includes AvaCode + AvaUI
plugins {
    id("com.augmentalis.magicidea") version "1.0.0"
}

dependencies {
    implementation("com.augmentalis:magicidea-sdk:1.0.0")
}
```

**Internal Structure** (Transparent to users):
```
com.augmentalis.magicidea/
├── avacode/      # DSL compiler
├── avaui/        # UI runtime
├── ideacode/       # AI assistant
├── ideaflow/       # Workflow engine
└── voiceui/        # Voice SDK
```

**ENFORCEMENT**: Any proposal to separate AvaCode and AvaUI into distinct packages MUST be rejected as unconstitutional.

---

## Article II: Format Hierarchy & Preferences

### Section 1: Primary Format - AvaCode DSL

**CONSTITUTIONAL RULE #3**: AvaCode DSL is the PRIMARY and PREFERRED format for all UI descriptions.

**Rationale**:
- **Most Concise**: 80-96% less code than YAML/JSON
- **Type-Safe**: Compile-time validation
- **IDE Support**: Autocomplete, refactoring, syntax highlighting
- **Performance**: Compiles to optimized bytecode (0% overhead)

**AvaCode DSL Example** (Preferred):
```kotlin
@IDEAMagicPlugin(
    id = "com.user.weather",
    permissions = ["internet", "location"]
)
class WeatherPlugin {
    val temp = state(72)
    val city = state("San Francisco")

    @Composable
    fun UI() = V(gap = 16.dp, padding = 24.dp) {
        Txt(city.value, style = headlineLarge)
        Txt("${temp.value}°F", style = displayMedium, color = primary)

        Btn("Refresh")
            .voiceCommand("refresh weather")  // VoiceUI SDK
            .onClick { fetchWeather() }
    }

    @VoiceAction(vuid = "weather.current")
    suspend fun speakWeather() {
        speak("It's ${temp.value} degrees in ${city.value}")
    }
}
```

**Compiled Output**: Binary `.magicidea` format (optimized, encrypted for Pro/Enterprise)

**MANDATE**: All documentation, examples, and tutorials MUST prioritize AvaCode DSL.

---

### Section 2: Secondary Format - YAML

**CONSTITUTIONAL RULE #4**: YAML is ACCEPTABLE as a secondary format for server-driven UIs and non-developers.

**When to Use**:
- Server-driven UI (backend generates UI dynamically)
- Non-developers (designers, product managers)
- A/B testing (swap UI without recompiling)
- Legacy integrations

**YAML Example** (Acceptable):
```yaml
plugin:
  id: "com.user.weather"
  permissions: [internet, location]

state:
  temp: 72
  city: "San Francisco"

ui:
  type: Column
  gap: 16dp
  padding: 24dp
  children:
    - type: Text
      text: "{{city}}"
      style: headlineLarge

    - type: Text
      text: "{{temp}}°F"
      style: displayMedium
      color: primary

    - type: Button
      text: "Refresh"
      voiceCommand: "refresh weather"
      onClick: {action: fetchWeather}

voiceActions:
  - vuid: "weather.current"
    handler: speakWeather
```

**PREFERENCE ORDER**: AvaCode DSL > YAML

---

### Section 3: Tertiary Format - JSON (Compact Arrays)

**CONSTITUTIONAL RULE #5**: JSON is ACCEPTABLE, but MUST use compact array format to minimize verbosity.

**Compact Array Format** (Mandated):
```json
{
  "plugin": {
    "id": "com.user.weather",
    "permissions": ["internet", "location"]
  },
  "ui": {
    "type": "Column",
    "gap": "16dp",
    "children": [
      ["Text", {"text": "{{city}}", "style": "headlineLarge"}],
      ["Text", {"text": "{{temp}}°F", "style": "displayMedium"}],
      ["Button", {
        "text": "Refresh",
        "voiceCommand": "refresh weather",
        "onClick": {"action": "fetchWeather"}
      }]
    ]
  },
  "voiceActions": [
    {"vuid": "weather.current", "handler": "speakWeather"}
  ]
}
```

**Format Rules**:
1. Component definition: `[ComponentType, {...props}]`
2. Children arrays: Always use array format
3. Simple values: Inline where possible

**PREFERENCE ORDER**: AvaCode DSL > YAML > Compact JSON

---

## Article III: VoiceUI SDK & Voice UUID (VUID)

### Section 1: VoiceUI SDK Architecture

**CONSTITUTIONAL RULE #6**: The VoiceUI SDK SHALL provide universal voice integration for all IDEAMagic applications.

**Core Capabilities**:
1. **VUID (Voice UUID)** - Global voice command routing leveraging existing uuidcreator
2. **Voice Commands** - Add voice control to any component
3. **Speech Recognition** - Powered by VoiceOS
4. **Text-to-Speech** - Natural voices, 40+ languages
5. **Voice Navigation** - Voice-driven app navigation
6. **Accessibility** - WCAG 2.1 AAA compliance

**Package**: `com.augmentalis.magicidea.voiceui`

**Foundation**: Built on existing `uuidcreator` library (`android/standalone-libraries/uuidcreator/`)

---

### Section 2: VUID (Voice UUID) System

**CONSTITUTIONAL RULE #7**: All voice actions SHALL be registered via Voice UUIDs (VUIDs) for cross-app voice control.

**VUID Format**: `{namespace}.{action}`

**Existing Infrastructure**: The VUID system leverages the existing `UUIDCreator` library, which already provides:
- Voice command processing (`processVoiceCommand`)
- Element registration with UUIDs
- Room database persistence
- Spatial navigation
- Action execution

**VUID Enhancement**: Simply add a license-based flag to enable cross-app routing:
```kotlin
// Existing: android/standalone-libraries/uuidcreator/
class UUIDCreator(context: Context) {
    var voiceRoutingEnabled: Boolean = false  // NEW: License flag

    fun enableVoiceRouting(licenseKey: String) {
        if (validateLicense(licenseKey, tier = Tier.PRO_OR_HIGHER)) {
            voiceRoutingEnabled = true
        }
    }
}
```

**Examples**:
- `weather.current` - Speak current weather
- `weather.refresh` - Refresh weather data
- `shopping.add` - Add item to shopping list
- `calendar.today` - Get today's events
- `email.send` - Send email

**VUID Registration**:
```kotlin
@VoiceAction(vuid = "weather.current")
suspend fun speakCurrentWeather() {
    val result = "It's ${temperature.value} degrees and ${forecast.value} in ${city.value}"
    speak(result)
}

// Or register programmatically
VoiceUI.registerAction(
    vuid = "weather.refresh",
    phrases = ["refresh weather", "update forecast", "get latest weather"],
    handler = ::fetchWeather
)
```

**Global Voice Routing**:
```
User: "Hey VoiceOS, what's the weather?"
  ↓
VoiceOS transcribes + NLP analysis
  ↓
Match to VUID: "weather.current"
  ↓
Route to WeatherPlugin (wake if needed)
  ↓
Execute: WeatherPlugin.speakCurrentWeather()
  ↓
VoiceUI SDK speaks result
```

**VUID Registry**:
- Maintained by VoiceOS system
- Plugins register on install
- User can view/manage all voice commands
- Conflict resolution (if multiple plugins register same phrase)

---

### Section 3: Voice Command Integration

**Voice-Enabled Components**:
```kotlin
// Add voice command to any component
Btn("Submit")
    .voiceCommand("submit", "send form", "submit form")
    .onClick { submitForm() }

// Voice input for text fields
Field(email, "Email")
    .voiceInput(enabled = true)
    .voiceCommand("enter email")

// Voice navigation
Screen()
    .voiceNav("go to settings", "open settings")
```

**Voice Workflow Integration** (IDEAFlow):
```kotlin
@IDEAFlow(
    trigger = "Voice command: 'morning routine'",
    actions = [
        "getWeather()",
        "getCalendarEvents()",
        "checkBirthdays()",
        "generateSummary()",
        "speakSummary()"
    ]
)
suspend fun morningRoutine() {
    // Automated via IDEAFlow + VoiceUI
}
```

---

### Section 4: VoiceUI SDK Monetization

**CONSTITUTIONAL RULE #8**: The VoiceUI SDK SHALL be offered in Free, Pro, and Enterprise tiers.

**Pricing Tiers**:

| Feature | Free | Pro ($14/month) | Enterprise ($99/month) |
|---------|------|-----------------|------------------------|
| Voice Commands | 1,000/month | Unlimited | Unlimited |
| VUID Registrations | 5 max | 50 max | Unlimited |
| Wake Word | "Hey VoiceOS" | Custom | Custom |
| Voices | 2 languages | 40 languages | 40 languages + custom |
| Routing Priority | Standard | Priority | Priority |
| Analytics | ❌ | ✅ | ✅ Advanced |
| On-Device AI | ❌ | ❌ | ✅ |
| White-Label | ❌ | ❌ | ✅ |
| SLA | ❌ | ❌ | 99.9% |

**License Validation**:
```kotlin
VoiceUI.initialize(
    context = context,
    licenseKey = "XXXX-XXXX-XXXX-XXXX",
    tier = VoiceTier.PRO
)
```

**Revenue Potential**:
- 10,000 Pro users × $14/month = $140K/month = **$1.68M/year**
- 100 Enterprise × $99/month = $9.9K/month = **$118K/year**
- **Total: $1.8M/year from VoiceUI SDK alone**

---

## Article IV: Plugin SDK Security & Encryption

### Section 1: Trust Levels

**CONSTITUTIONAL RULE #9**: All plugins SHALL be assigned one of three trust levels: UNTRUSTED, VERIFIED, or TRUSTED.

**Trust Level Definitions**:

```
┌─────────────────────────────────────────────────────────────┐
│ TIER 1: UNTRUSTED (Default)                                 │
├─────────────────────────────────────────────────────────────┤
│ • All 3rd-party plugins start here                          │
│ • Isolated storage only                                     │
│ • Cannot access system data                                 │
│ • Cannot provide capabilities                               │
│ • User approval required for all permissions                │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ TIER 2: VERIFIED (User-Approved Capabilities)               │
├─────────────────────────────────────────────────────────────┤
│ • Plugins with user-approved permissions                    │
│ • Can access system APIs (contacts, calendar, etc.)         │
│ • Can request data from other plugins                       │
│ • Can consume capabilities                                  │
│ • Explicit user consent required                            │
│ • Revocable at any time                                     │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ TIER 3: TRUSTED (Avanues Official + Certified)          │
├─────────────────────────────────────────────────────────────┤
│ • Official Avanues plugins                              │
│ • Certified partner plugins (audited)                       │
│ • Can provide capabilities to other plugins                 │
│ • Signed by Avanues (code-signing certificate)         │
│ • Audited for security                                      │
│ • Can register TRUSTED VUIDs                               │
└─────────────────────────────────────────────────────────────┘
```

---

### Section 2: Capability System (Inter-Plugin Communication)

**CONSTITUTIONAL RULE #10**: Plugins SHALL communicate via the capability system, NOT direct function calls.

**Capability Architecture**:

```kotlin
// ============================================================
// CAPABILITY PROVIDER (TRUSTED Plugin)
// ============================================================

@IDEAMagicPlugin(
    id = "com.avanues.contacts",
    trustLevel = TrustLevel.TRUSTED,
    providesCapabilities = ["contacts.read", "contacts.search"]
)
class ContactsPlugin : CapabilityProvider {

    @CapabilityHandler("contacts.search")
    suspend fun searchContacts(
        query: String,
        requestingPlugin: PluginInfo
    ): List<Contact> {
        // Check authorization
        if (!isAuthorized(requestingPlugin, "contacts.search")) {
            throw SecurityException("Not authorized")
        }

        // Log for audit
        auditLog.log("contacts.search", requestingPlugin.id, query)

        // Return data
        return contactsDatabase.search(query)
    }
}

// ============================================================
// CAPABILITY CONSUMER (UNTRUSTED Plugin)
// ============================================================

@IDEAMagicPlugin(
    id = "com.user.birthday_reminder",
    trustLevel = TrustLevel.UNTRUSTED,
    requiresCapabilities = ["contacts.search"]
)
class BirthdayPlugin {

    @Composable
    fun UI() = V {
        Btn("Find Birthdays") {
            requestCapability("contacts.search") { granted ->
                if (granted) {
                    val contacts = searchContacts("birthday")
                    // Display contacts
                } else {
                    showToast("Permission denied")
                }
            }
        }
    }

    suspend fun searchContacts(query: String): List<Contact> {
        return CapabilitySystem.invoke(
            capability = "contacts.search",
            provider = "com.avanues.contacts",
            params = mapOf("query" to query)
        )
    }
}
```

**User Consent Flow**:
```
1. User installs Birthday Reminder plugin
   ↓
2. Plugin declares: requiresCapabilities = ["contacts.search"]
   ↓
3. Avanues shows permission prompt:
   ┌─────────────────────────────────────────────────┐
   │ 🔐 Permission Request                           │
   │                                                 │
   │ "Birthday Reminder" wants to:                   │
   │   • Search your contacts                        │
   │                                                 │
   │ Provided by: Contacts Manager (Official)       │
   │ Risk Level: MEDIUM                              │
   │                                                 │
   │ [Allow Once] [Allow Always] [Deny]              │
   └─────────────────────────────────────────────────┘
   ↓
4. User approves → Capability granted
   ↓
5. All invocations logged for audit
```

**Capability Registry**:
```kotlin
object CapabilityRegistry {
    // Register capability provider
    fun registerProvider(plugin: PluginInfo, capabilities: List<String>)

    // Request capability access
    suspend fun requestCapability(plugin: PluginInfo, capability: String): CapabilityGrant

    // Invoke capability (mediated call)
    suspend fun invoke(plugin: PluginInfo, capability: String, params: Map<String, Any>): Any

    // Revoke capability
    fun revoke(plugin: PluginInfo, capability: String)
}
```

**Security Guarantees**:
- ✅ User control (explicit approval required)
- ✅ Audit trail (all invocations logged)
- ✅ Rate limiting (prevent abuse)
- ✅ Revocable (user can revoke anytime)
- ✅ Sandboxed (isolated execution context)

---

### Section 3: Plugin SDK Encryption

**CONSTITUTIONAL RULE #11**: The IDEAMagic Plugin SDK SHALL be distributed as an encrypted library to prevent theft and reverse engineering.

**Distribution Format**:
```
magicidea-plugin-sdk-1.0.0/
├── magicidea-core.aar              # Free (Apache 2.0)
├── magicidea-plugin-sdk-pro.aar    # Encrypted (DexGuard)
├── voiceui-sdk.aar                 # Encrypted (DexGuard)
└── LICENSE.txt
```

**Encryption Layers**:

| Layer | Technology | Protection | Overhead |
|-------|-----------|-----------|----------|
| 1. ProGuard Obfuscation | R8 | Low (delays 1 day) | 0% |
| 2. String Encryption | DexGuard | Medium (1 week) | 5% |
| 3. Class Encryption | DexGuard | High (1 month) | 10% |
| 4. License Validation | Server | Very High (blocks pirates) | 200ms (once) |
| 5. Legal (EULA) | Contract | Ultimate | 0% |

**Total Protection**: Delays reverse engineering ~1 month, blocks 95% of pirates.

**License Validation**:
```kotlin
IDEAMagic.initialize(
    context = context,
    licenseKey = "XXXX-XXXX-XXXX-XXXX",
    onValidated = { tier ->
        when (tier) {
            Tier.FREE -> enableFreeFeatures()
            Tier.PRO -> enableProFeatures()
            Tier.ENTERPRISE -> enableEnterpriseFeatures()
        }
    },
    onInvalid = {
        throw SecurityException("Invalid license")
    }
)
```

---

## Article V: App Store Compliance

### Section 1: No Dynamic Code Execution

**CONSTITUTIONAL RULE #12**: Plugins SHALL NOT execute arbitrary code. Only data interpretation is allowed.

**Banned**:
- ❌ Downloading `.dex`, `.so`, `.dylib` files
- ❌ `eval()`, `exec()`, `ScriptEngine`
- ❌ Loading external JARs/AARs
- ❌ WebView `evaluateJavascript()` for logic
- ❌ JIT compilation

**Allowed**:
- ✅ Downloading JSON/YAML/AvaCode DSL data
- ✅ Parsing data into UI tree
- ✅ Predefined action types (whitelist)
- ✅ Server-driven UI
- ✅ A/B testing (different data)

**Compliance Workflow**:
```
Desktop: User writes AvaCode DSL
  ↓
Desktop: IDEAMagic compiler generates .magicidea file (DATA)
  ↓
Desktop: Upload to Avanues Plugin Server
  ↓
Phone: Download .magicidea file (just data, not executable)
  ↓
Phone: IDEAMagic runtime parses data
  ↓
Phone: Render UI based on data (NOT execute code)
  ↓
Phone: Execute ONLY predefined actions (whitelist)
```

**Key Distinction**:
```
❌ Code Execution: Download → Execute → Run arbitrary logic
✅ Data Interpretation: Download → Parse → Render predefined components
```

---

### Section 2: Predefined Action Whitelist

**CONSTITUTIONAL RULE #13**: Plugins MAY only use predefined action types from the whitelist.

**Approved Action Types** (12):

1. **http** - HTTP requests (GET, POST, PUT, DELETE, PATCH)
2. **database** - Local SQLite operations (insert, update, delete, query)
3. **navigate** - Screen navigation
4. **showToast** - UI feedback (toast, snackbar, alert)
5. **math** - Mathematical expressions (safe eval)
6. **conditional** - If/else logic
7. **loop** - Iteration (map, filter, reduce)
8. **intent** - Platform intents (Android)
9. **urlScheme** - URL schemes (iOS)
10. **storage** - Key-value storage (get, set, delete)
11. **speech** - Voice I/O (speak, recognize) - **VoiceUI SDK**
12. **location** - GPS access (getCurrent, watchPosition)

**Example Usage**:
```yaml
actions:
  fetchWeather:
    type: http
    method: GET
    url: "https://api.weather.com/v3?city={{city}}"
    responseMapping:
      temperature: "$.current.temp"
      forecast: "$.current.conditions"

  speakResult:
    type: speech
    operation: speak
    text: "It's {{temperature}} degrees and {{forecast}}"
```

**ENFORCEMENT**: Any action type NOT on this whitelist MUST be rejected at parse time.

**Adding New Action Types**: Requires constitutional amendment.

---

## Article VI: Performance & Quality Standards

### Section 1: Performance Targets

**CONSTITUTIONAL RULE #14**: IDEAMagic System MUST meet the following performance targets.

| Metric | Target | Measurement |
|--------|--------|-------------|
| **UI Update Latency** | <1ms (99th percentile) | Android/iOS Profiler |
| **Frame Rate** | 60 FPS minimum | Frame stats |
| **Memory Overhead** | <5MB | Memory profiler |
| **App Size Increase** | <3MB compressed | APK/IPA analyzer |
| **Compile Time** | <30s for 10K LOC | Build timer |
| **Cold Start** | <100ms overhead | Startup profiler |
| **Voice Recognition** | <500ms latency | VoiceUI SDK metrics |
| **VUID Routing** | <50ms | VoiceOS router |

**ENFORCEMENT**: Any PR degrading performance by >10% MUST be rejected.

**Optimization Techniques** (Required):
1. Inline functions (zero lambda allocations)
2. Value classes (50% memory reduction)
3. Immutable data structures (structural sharing)
4. Lazy composition (defer rendering)
5. Compile-time code generation (0% runtime overhead)

---

### Section 2: Quality Standards

**Test Coverage**: 80% minimum (JaCoCo)
**Documentation**: KDoc for all public APIs
**Null Safety**: Zero `!!` operators in production code
**Accessibility**: WCAG 2.1 AAA compliance (voice-enabled)
**Security**: Regular penetration testing (quarterly)

---

## Article VII: Versioning & Compatibility

### Section 1: Semantic Versioning

**Format**: `MAJOR.MINOR.PATCH` (e.g., `1.2.3`)

- **MAJOR**: Breaking changes
- **MINOR**: New features (backwards compatible)
- **PATCH**: Bug fixes

**Deprecation Policy**:
1. Mark as `@Deprecated` in version N
2. Keep working for 2 minor versions
3. Remove in MAJOR version upgrade

**Cross-Component Versioning**:
```kotlin
// All components versioned together
implementation("com.augmentalis:magicidea-sdk:1.0.0")
// Includes: AvaCode 1.0.0, AvaUI 1.0.0, IDEACode 1.0.0,
//           IDEAFlow 1.0.0, VoiceUI 1.0.0
```

---

## Article VIII: License Tiers & Monetization

### Section 1: License Tiers

**CONSTITUTIONAL RULE #15**: IDEAMagic System SHALL be offered in Free, Pro, and Enterprise tiers.

**Free Tier** (Apache 2.0):
```
AvaCode/AvaUI:
• 15 basic components
• Material 3 theme
• Android + Desktop renderers
• State management
• YAML/JSON parser

IDEACode:
• 50 AI generations/month

IDEAFlow:
• 10 workflows

VoiceUI SDK:
• 1,000 voice commands/month
• 5 VUID registrations
• Standard wake word
• 2 languages
```

**Pro Tier** ($199/year for SDK + $14/month for VoiceUI = **$367/year**):
```
AvaCode/AvaUI:
• 50 components (all advanced)
• 7 platform themes
• iOS + Web renderers
• Visual theme builder
• Asset manager (2,400+ icons)

IDEACode:
• Unlimited AI generations
• Priority processing

IDEAFlow:
• Unlimited workflows
• Advanced triggers

VoiceUI SDK:
• Unlimited voice commands
• 50 VUID registrations
• Custom wake word
• 40 languages
• Priority routing
• Analytics
```

**Enterprise Tier** ($2,999/year for SDK + $99/month for VoiceUI = **$4,187/year**):
```
Everything in Pro PLUS:
• Source code access
• White-label branding
• On-premise license server
• On-device AI (VoiceUI)
• Custom voice model training
• Multi-user support
• SLA (99.9% uptime)
• Priority support
```

---

### Section 2: Revenue Projections

**Year 1** (Conservative):
- 5,000 Free users → $0 (ecosystem growth)
- 1,000 Pro users → $367K/year
- 50 Enterprise → $209K/year
- **Total: $576K/year**

**Year 3** (Growth):
- 50,000 Free users → $0
- 10,000 Pro users → $3.67M/year
- 500 Enterprise → $2.09M/year
- **Total: $5.76M/year**

---

## Article IX: Intellectual Property & Trademarks

### Section 1: Trademarks

**"IDEAMagic"** is a registered trademark of Augmentalis LLC.

**Sub-brands**:
- "AvaCode" (DSL Language)
- "AvaUI" (UI Framework)
- "IDEACode" (AI Assistant)
- "IDEAFlow" (Workflow Engine)
- "VoiceUI SDK" (Voice Integration)

**Allowed Uses**:
- ✅ "Built with IDEAMagic"
- ✅ "Compatible with IDEAMagic"
- ✅ "IDEAMagic Plugin"

**Prohibited Uses**:
- ❌ "IDEAMagic Pro" (implies official product)
- ❌ "IDEAMagic Enterprise" (trademark infringement)

---

### Section 2: Open Source Components

**Apache 2.0 Components** (Free Tier):
- AvaCode DSL parser (YAML/JSON)
- AvaUI Runtime (15 components)
- State management system
- Android + Desktop renderers

**Proprietary Components** (Pro/Enterprise):
- Advanced components (35)
- iOS + Web renderers
- Visual theme builder
- IDEACode AI engine
- IDEAFlow workflow engine
- VoiceUI SDK (all tiers)

---

## Article X: Governance & Amendments

### Section 1: Authority

**Benevolent Dictator**: Manoj Jhawar (manoj@ideahq.net)
- Final decision on all constitutional matters
- Can veto any proposal
- Can amend constitution unilaterally

**Advisory Board** (Future):
- 5 community members
- 2 enterprise customers
- 1 security expert
- 1 voice/AI expert
- Voting power: Advisory only (no veto)

---

### Section 2: Amendment Process

**Proposing Amendment**:
1. Submit GitHub issue: "Constitutional Amendment: [Title]"
2. Rationale (why change needed)
3. Impact analysis (breaking changes?)
4. Community discussion (14 days minimum)
5. Advisory board vote (majority approval)
6. Final approval by Benevolent Dictator

**Examples of Valid Amendments**:
- Adding new action type to whitelist
- Changing performance targets
- New component tier (e.g., "Magic Tier")
- New voice language support

**Examples of Invalid Amendments**:
- Separating AvaCode and AvaUI (violates Article I)
- Removing encryption requirement (violates Article IV)
- Allowing arbitrary code execution (violates Article V)

---

## Article XI: Security & Privacy

### Section 1: Data Collection

**IDEAMagic SDK**:
- ✅ Collects: Error reports (opt-in), performance metrics (opt-in)
- ❌ Does NOT collect: Personal data, user input, location

**VoiceUI SDK**:
- ✅ Collects: Voice command usage (opt-in), VUID invocations (analytics)
- ❌ Does NOT collect: Voice recordings (processed on-device), personal conversations

**License Server**:
- ✅ Stores: License key, package name, expiration date
- ❌ Does NOT store: Source code, user data, voice recordings

**GDPR Compliance**:
- Right to deletion (delete all data)
- Right to access (export data)
- No third-party sharing
- Data residency options (Enterprise)

---

### Section 2: Voice Privacy

**CONSTITUTIONAL RULE #16**: Voice recordings SHALL be processed on-device by default. Cloud processing requires explicit user consent.

**Voice Processing**:
- **Default**: On-device processing (privacy-first)
- **Optional**: Cloud processing (better accuracy, requires consent)
- **Never**: Permanent storage of voice recordings

**VUID Privacy**:
- VUID invocations logged (for debugging/analytics)
- NO voice audio stored
- NO personal information in logs
- User can disable logging (Settings)

---

## Article XII: Ratification

This Constitution is hereby ratified and takes effect immediately.

**Effective Date**: 2025-11-01 12:32 PST

**Supersedes**: MAGICUI-CONSTITUTION-251101-1155.md

**Signatories**:
- Manoj Jhawar, Founder & CEO, Augmentalis LLC

**Witnesses**:
- Claude (AI Assistant, Anthropic) - Technical Advisor

---

## Appendix A: Quick Reference

### IDEAMagic System Components
1. **AvaCode** - DSL Language (96% less code)
2. **AvaUI** - UI Framework (50 components, 6 platforms)
3. **IDEACode** - AI Coding Assistant (natural language → code)
4. **IDEAFlow** - Workflow Engine (visual automation)
5. **VoiceUI SDK** - Voice Integration (VUID system)

### Format Preference
1. **AvaCode DSL** (Primary) - Type-safe, concise, 0% overhead
2. **YAML** (Secondary) - Human-readable, server-driven
3. **Compact JSON** (Tertiary) - REST APIs, databases

### Trust Levels
1. **UNTRUSTED** - Default, isolated sandbox
2. **VERIFIED** - User-approved capabilities
3. **TRUSTED** - Official Avanues plugins

### Predefined Actions (12)
http, database, navigate, showToast, math, conditional, loop, intent, urlScheme, storage, speech, location

### Performance Targets
- UI Update: <1ms
- Frame Rate: 60 FPS
- Memory: <5MB
- Voice Recognition: <500ms
- VUID Routing: <50ms

### License Tiers
- **Free**: Apache 2.0
- **Pro**: $367/year (SDK + VoiceUI)
- **Enterprise**: $4,187/year (+ source access)

### Revenue Potential
- **Year 1**: $576K
- **Year 3**: $5.76M

---

## Appendix B: Example Application

### Complete Voice-Enabled Weather App (30 lines)

```kotlin
import com.augmentalis.magicidea.*

@IDEAMagicApp(
    id = "com.example.weather",
    voice = VoiceConfig(wakeWord = "Hey Weather")
)
class WeatherApp {
    val city = state("San Francisco")
    val temp = state(72)
    val forecast = state("Sunny")

    @Composable
    fun UI() = V(gap = 16.dp, padding = 24.dp) {
        Txt(city.value, style = headlineLarge)
        Txt("${temp.value}°F", style = displayMedium, color = primary)
        Txt(forecast.value, style = bodyLarge)

        Btn("Refresh", icon = "refresh")
            .voiceCommand("refresh", "update")
            .onClick { fetchWeather() }

        Field(city, "City").voiceInput(enabled = true)
    }

    @IDEAFlow(trigger = "Every 30 min OR voice 'refresh'")
    suspend fun fetchWeather() {
        val response = http.get("https://api.weather.com/v3?city=${city.value}")
        temp.value = response.json["temp"].asInt()
        forecast.value = response.json["forecast"].asString()
        speak("Weather updated. ${temp.value} degrees and ${forecast.value}")
    }

    @VoiceAction(vuid = "weather.current")
    suspend fun speakWeather() {
        speak("It's ${temp.value} degrees and ${forecast.value} in ${city.value}")
    }
}
```

**Lines of Code Comparison**:
- IDEAMagic: **30 lines**
- Unity: ~800 lines
- React Native: ~200 lines
- Flutter: ~150 lines
- Jetpack Compose: ~120 lines

**Code Reduction**: 96% less than Unity, 85% less than React Native, 80% less than Flutter

---

## Appendix C: VUID Standard Examples

### Weather Plugin
```
weather.current    - Speak current weather
weather.refresh    - Refresh weather data
weather.forecast   - Speak forecast
weather.location   - Change location
```

### Shopping Plugin
```
shopping.add       - Add item to list
shopping.list      - Read shopping list
shopping.remove    - Remove item
shopping.clear     - Clear list
```

### Calendar Plugin
```
calendar.today     - Get today's events
calendar.tomorrow  - Get tomorrow's events
calendar.add       - Add new event
calendar.next      - Get next event
```

### Email Plugin
```
email.send         - Send email
email.read         - Read latest email
email.unread       - Get unread count
email.search       - Search emails
```

---

## Appendix D: Enforcement Checklist

**Constitutional Compliance** (CI/CD):
- [ ] Article I: Single package (AvaCode + AvaUI unified)
- [ ] Article II: DSL examples in docs (not just YAML)
- [ ] Article III: VoiceUI SDK integrated
- [ ] Article IV: Plugin SDK encrypted (DexGuard)
- [ ] Article V: No code execution (only data interpretation)
- [ ] Article VI: Performance tests pass (<1ms, 60 FPS)
- [ ] Article VII: Semantic versioning followed
- [ ] Article VIII: License tiers implemented
- [ ] Article IX: Trademark headers present
- [ ] Article X: Amendment process documented
- [ ] Article XI: Privacy policy compliant
- [ ] Article XII: Ratification signed

---

**Document Status**: ACTIVE - All implementations MUST comply
**Next Review**: 2026-11-01 (annual review)

**Created by Manoj Jhawar, manoj@ideahq.net**
**IDEAMagic System - Where Magic Meets Intelligence**
