# AvaUI/AvaCode System Constitution
**Document Type:** Master Constitution (Living Document)
**Created:** 2025-11-01 11:55 PST
**Status:** AUTHORITATIVE - All implementations MUST comply
**Version:** 1.0.0
**Authority:** Manoj Jhawar, manoj@ideahq.net

---

## Preamble

This Constitution establishes the foundational principles, architectural decisions, and immutable rules for the AvaUI/AvaCode ecosystem. All code, documentation, and decisions MUST align with these principles.

**Purpose**: Define a declarative UI system that is:
1. **Concise** - 80-96% less code than competitors
2. **Secure** - App Store compliant, sandboxed, encrypted
3. **Performant** - <1ms latency, 0% runtime overhead
4. **Universal** - Single codebase, 6+ platforms
5. **Monetizable** - Free + Pro + Enterprise tiers

---

## Article I: Unified Package Principle

### Section 1: Single Package Mandate

**CONSTITUTIONAL RULE #1**: AvaUI and AvaCode SHALL be a single unified package.

**Rationale**:
- They are inseparable (AvaCode generates AvaUI, AvaUI consumes AvaCode output)
- Version synchronization is critical (breaking changes affect both)
- User simplicity (one dependency, not two)
- Industry standard (React, Flutter, Compose all unified)

**Package Name**: `com.augmentalis:avaui`

**Internal Structure**:
```
com.augmentalis.avaui/
├── runtime/              # AvaUI components (user-facing)
│   ├── components/       # Btn, Txt, Field, V, H, etc.
│   ├── theme/           # Theme system
│   ├── state/           # State management
│   └── annotations/     # @Magic annotation
│
├── compiler/            # AvaCode (internal, not exposed)
│   ├── ksp/            # KSP processor
│   ├── parser/         # DSL/YAML/JSON parser
│   ├── codegen/        # Code generator
│   └── optimizer/      # Performance optimizer
│
├── gradle-plugin/      # Gradle integration
│   └── AvaUIPlugin   # Auto-applies KSP, configures compiler
│
└── plugin-sdk/         # For plugin authors (Avanues)
    ├── PluginAPI       # Avanues APIs (http, location, speech, etc.)
    ├── AvanuesPlugin  # Base class
    └── annotations/    # Plugin metadata annotations
```

**User Dependency** (Simple, One-Line):
```kotlin
// build.gradle.kts
plugins {
    id("com.augmentalis.avaui") version "1.0.0"
}

dependencies {
    implementation("com.augmentalis:avaui:1.0.0")  // Includes everything
}
```

**Versioning**: Atomic releases (runtime + compiler + plugin-sdk versioned together)
- v1.0.0 - Initial release
- v1.1.0 - New components (backwards compatible)
- v2.0.0 - Breaking changes (all parts updated together)

**ENFORCEMENT**: Any proposal to separate AvaUI and AvaCode into separate packages MUST be rejected as unconstitutional.

---

## Article II: Format Hierarchy & Preferences

### Section 1: Primary Format - AvaCode DSL

**CONSTITUTIONAL RULE #2**: AvaCode DSL is the PRIMARY and PREFERRED format for all UI descriptions.

**Rationale**:
- **Most Concise**: 80-96% less code than YAML/JSON
- **Type-Safe**: Compile-time validation
- **IDE Support**: Autocomplete, syntax highlighting, refactoring
- **Performance**: Compiles to optimized bytecode (0% overhead)

**AvaCode DSL Example** (Preferred):
```kotlin
// WeatherPlugin.kt
@AvanuesPlugin(
    id = "com.user.weather",
    name = "Weather Widget",
    version = "1.0.0",
    permissions = ["internet", "location"]
)
class WeatherPlugin {
    val city = state("San Francisco")
    val temperature = state(72)
    val iconUrl = state("https://...")

    @Composable
    fun UI() = V(gap = 16.dp, padding = 24.dp) {
        Txt(city.value, style = headlineLarge)
        Txt("${temperature.value}°F", style = displayMedium, color = primary)
        Img(src = iconUrl.value, w = 64.dp, h = 64.dp)
        Btn("Refresh") {
            fetchWeather()
        }
    }

    suspend fun fetchWeather() {
        val response = http.get("https://api.weather.com/v3/weather?city=${city.value}")
        temperature.value = response.json["current"]["temp"].asInt()
        iconUrl.value = response.json["current"]["icon"].asString()
    }
}
```

**Compiled Output**: `plugin_weather.avaui` (binary format, optimized)

**Why AvaCode DSL is Superior**:
| Metric | AvaCode DSL | YAML | JSON |
|--------|--------------|------|------|
| **Lines of Code** | 20 | 80 | 120 |
| **Type Safety** | ✅ Compile-time | ❌ Runtime | ❌ Runtime |
| **IDE Support** | ✅ Full | 🟡 Limited | 🟡 Limited |
| **Compile-Time** | ✅ 0% overhead | ❌ 5ms parse | ❌ 10ms parse |
| **Error Detection** | ✅ Before run | ❌ At runtime | ❌ At runtime |
| **Refactoring** | ✅ IDE refactor | ❌ Manual | ❌ Manual |

**MANDATE**: All documentation, examples, and tutorials MUST prioritize AvaCode DSL.

---

### Section 2: Secondary Format - YAML

**CONSTITUTIONAL RULE #3**: YAML is ACCEPTABLE as a secondary format for server-driven UIs and non-developers.

**When to Use YAML**:
1. Server-driven UI (backend generates UI dynamically)
2. Non-developers (designers, product managers)
3. A/B testing (swap UI without recompiling)
4. Legacy systems (existing YAML configurations)

**YAML Example** (Acceptable):
```yaml
# plugin_weather.yaml
plugin:
  id: "com.user.weather"
  name: "Weather Widget"
  version: "1.0.0"
  permissions:
    - internet
    - location

state:
  city: "San Francisco"
  temperature: 72
  iconUrl: "https://..."

ui:
  type: Column
  gap: 16dp
  padding: 24dp
  children:
    - type: Text
      text: "{{city}}"
      style: headlineLarge

    - type: Text
      text: "{{temperature}}°F"
      style: displayMedium
      color: primary

    - type: Image
      src: "{{iconUrl}}"
      width: 64dp
      height: 64dp

    - type: Button
      text: "Refresh"
      onClick:
        action: fetchWeather

actions:
  fetchWeather:
    type: http
    method: GET
    url: "https://api.weather.com/v3/weather?city={{city}}"
    responseMapping:
      temperature: "$.current.temp"
      iconUrl: "$.current.icon"
```

**YAML Constraints**:
- ✅ Human-readable
- ✅ Comments allowed
- ❌ No compile-time validation
- ❌ Slower parsing (5ms overhead)
- ❌ No IDE refactoring

**PREFERENCE ORDER**: AvaCode DSL > YAML

---

### Section 3: Tertiary Format - JSON (Compact Arrays)

**CONSTITUTIONAL RULE #4**: JSON is ACCEPTABLE, but MUST use compact array format where possible to minimize verbosity.

**When to Use JSON**:
1. REST APIs (standardized format)
2. Database storage (PostgreSQL JSONB, MongoDB)
3. Browser-based tools (JavaScript native)
4. Legacy integrations

**JSON Compact Array Format** (Mandated):
```json
{
  "plugin": {
    "id": "com.user.weather",
    "name": "Weather Widget",
    "version": "1.0.0",
    "permissions": ["internet", "location"]
  },
  "state": {
    "city": "San Francisco",
    "temperature": 72,
    "iconUrl": "https://..."
  },
  "ui": {
    "type": "Column",
    "gap": "16dp",
    "padding": "24dp",
    "children": [
      ["Text", {"text": "{{city}}", "style": "headlineLarge"}],
      ["Text", {"text": "{{temperature}}°F", "style": "displayMedium", "color": "primary"}],
      ["Image", {"src": "{{iconUrl}}", "width": "64dp", "height": "64dp"}],
      ["Button", {"text": "Refresh", "onClick": {"action": "fetchWeather"}}]
    ]
  },
  "actions": {
    "fetchWeather": {
      "type": "http",
      "method": "GET",
      "url": "https://api.weather.com/v3/weather?city={{city}}",
      "responseMapping": {
        "temperature": "$.current.temp",
        "iconUrl": "$.current.icon"
      }
    }
  }
}
```

**Compact Array Format Rules**:
1. **Component Definition**: `[ComponentType, {...props}]`
   - Example: `["Button", {"text": "Click", "onClick": {...}}]`
   - NOT: `{"type": "Button", "text": "Click", "onClick": {...}}`

2. **Children Arrays**: Always use array format
   - Example: `"children": [["Text", {...}], ["Button", {...}]]`
   - NOT: `"children": [{"type": "Text", ...}, {"type": "Button", ...}]`

3. **Simple Values**: Inline where possible
   - Example: `"color": "primary"` (not `"color": {"name": "primary"}`)
   - Example: `"gap": "16dp"` (not `"gap": {"value": 16, "unit": "dp"}`)

**Comparison**:
```json
// ❌ VERBOSE JSON (BANNED)
{
  "ui": {
    "type": "Column",
    "children": [
      {
        "type": "Text",
        "properties": {
          "text": "Hello",
          "style": "headlineLarge"
        }
      },
      {
        "type": "Button",
        "properties": {
          "text": "Click",
          "onClick": {
            "action": "submit"
          }
        }
      }
    ]
  }
}

// ✅ COMPACT JSON (REQUIRED)
{
  "ui": {
    "type": "Column",
    "children": [
      ["Text", {"text": "Hello", "style": "headlineLarge"}],
      ["Button", {"text": "Click", "onClick": {"action": "submit"}}]
    ]
  }
}
```

**Size Reduction**: Compact format reduces JSON size by ~40%.

**PREFERENCE ORDER**: AvaCode DSL > YAML > Compact JSON

**ENFORCEMENT**: Any JSON parser MUST support compact array format. Verbose format MAY be accepted for backwards compatibility but MUST be converted to compact format internally.

---

## Article III: Plugin SDK Security & Encryption

### Section 1: Plugin SDK as Encrypted Library

**CONSTITUTIONAL RULE #5**: The Avanues Plugin SDK SHALL be distributed as an encrypted library to prevent theft and reverse engineering.

**Rationale**:
- Plugin SDK contains proprietary Avanues APIs
- Competitors could steal the API design
- Malicious actors could create fake plugins
- Intellectual property protection
- Revenue protection (Pro/Enterprise tiers)

**Distribution Format**:
```
avanues-plugin-sdk-1.0.0/
├── avaui-1.0.0.aar               # Open source (Apache 2.0)
├── avanues-sdk-core-1.0.0.aar  # Encrypted (DexGuard) ✅
├── avanues-sdk-pro-1.0.0.aar   # Encrypted (DexGuard) ✅
└── LICENSE.txt                      # EULA
```

**Encryption Strategy**:

1. **Code Obfuscation** (ProGuard/R8)
   ```
   com.augmentalis.avanues.PluginAPI → a.b.c.A
   fun httpGet(url: String) → fun a(s: String)
   ```

2. **String Encryption** (DexGuard)
   ```kotlin
   // Source:
   const val API_BASE = "https://api.avanues.com"

   // Encrypted:
   const val API_BASE = decrypt("aGVsbG8gd29ybGQ=", KEY)
   ```

3. **Class Encryption** (DexGuard)
   - Encrypt entire classes
   - Decrypt on-demand at runtime
   - 60% slower startup (acceptable)

4. **Asset Encryption** (Custom)
   - Encrypt bundled resources
   - API documentation (embedded HTML)
   - Example code

5. **License Validation** (Server-side)
   ```kotlin
   // Plugin SDK requires license key
   AvanuesSDK.initialize(
       context = context,
       licenseKey = "XXXX-XXXX-XXXX-XXXX",  // Validated via server
       onValidated = { tier ->
           when (tier) {
               Tier.FREE -> enableFreeFeatures()
               Tier.PRO -> enableProFeatures()
               Tier.ENTERPRISE -> enableEnterpriseFeatures()
           }
       },
       onInvalid = {
           throw SecurityException("Invalid license key")
       }
   )
   ```

**Protection Layers**:
| Layer | Technology | Protection Level | Performance Impact |
|-------|-----------|------------------|-------------------|
| 1. ProGuard Obfuscation | R8 | Low (delays 1 day) | 0% |
| 2. String Encryption | DexGuard | Medium (delays 1 week) | 5% |
| 3. Class Encryption | DexGuard | High (delays 1 month) | 10% |
| 4. License Validation | Server | Very High (blocks pirates) | 200ms (one-time) |
| 5. Legal (EULA) | Contract | Ultimate | 0% |

**Total Protection**: Delays reverse engineering by ~1 month, blocks 95% of casual pirates.

**REALITY CHECK**: Determined attackers WILL crack it eventually. Goal is to make it expensive/time-consuming enough that it's not worth it.

---

### Section 2: License Tiers

**Free Tier** (Open Source):
- `com.augmentalis:avaui-core` (Apache 2.0)
- 15 basic components
- Material 3 theme
- Android + Desktop renderers
- No encryption (open source)

**Pro Tier** ($199/year):
- `com.augmentalis:avaui-pro` (Encrypted)
- 50 components (all advanced)
- 7 platform themes
- iOS + Web renderers
- License key required
- Email support

**Enterprise Tier** ($2,999/year):
- `com.augmentalis:avaui-enterprise` (Encrypted + Source Access)
- Custom theme builder
- White-label branding
- Source code access (upon request)
- Priority support + SLA
- License key required

**Plugin SDK Access**:
- Free: `avanues-plugin-sdk-free` (limited APIs)
- Pro: `avanues-plugin-sdk-pro` (full APIs, encrypted)
- Enterprise: `avanues-plugin-sdk-enterprise` (source access)

**License Validation** (Server-side):
```kotlin
// SDK makes HTTPS request on initialization
POST https://license.avanues.com/validate
{
    "licenseKey": "XXXX-XXXX-XXXX-XXXX",
    "packageName": "com.mycompany.myapp",
    "deviceId": "abc123...",
    "sdkVersion": "1.0.0"
}

// Response:
{
    "valid": true,
    "tier": "PRO",
    "expiresAt": "2026-11-01T00:00:00Z",
    "features": ["advanced_components", "ios_renderer", "custom_themes"]
}
```

**Offline Mode**: Cache validation for 7 days (prevents "phone home" every launch).

---

### Section 3: Anti-Tampering

**Tamper Detection**:
```kotlin
object TamperDetection {
    fun verify() {
        // 1. Signature verification
        val expectedSignature = "SHA256:abc123..."
        val actualSignature = getAPKSignature()
        if (actualSignature != expectedSignature) {
            throw SecurityException("APK signature mismatch")
        }

        // 2. Checksum verification
        val expectedChecksum = "MD5:def456..."
        val actualChecksum = calculateChecksum()
        if (actualChecksum != expectedChecksum) {
            throw SecurityException("SDK tampered")
        }

        // 3. Root/Jailbreak detection
        if (isDeviceRooted()) {
            log.warn("Running on rooted device (higher piracy risk)")
            // Don't block, just log (false positives on custom ROMs)
        }

        // 4. Debugger detection
        if (isDebuggerAttached()) {
            throw SecurityException("Debugger detected")
        }
    }
}
```

**Response to Tampering**:
- **Free Tier**: Warning message (don't block, open source)
- **Pro Tier**: Disable Pro features, fall back to Free
- **Enterprise Tier**: Contact license server, notify admin

---

## Article IV: App Store Compliance

### Section 1: No Dynamic Code Execution

**CONSTITUTIONAL RULE #6**: Plugins SHALL NOT execute arbitrary code. Only data interpretation is allowed.

**Banned**:
- ❌ Downloading `.dex`, `.so`, `.dylib` files
- ❌ `eval()`, `exec()`, `ScriptEngine`
- ❌ Loading external JARs/AARs
- ❌ WebView `evaluateJavascript()` for logic
- ❌ JIT compilation
- ❌ Reflection to invoke arbitrary methods

**Allowed**:
- ✅ Downloading JSON/YAML/AvaUI data
- ✅ Parsing data into UI tree
- ✅ Predefined action types (whitelist)
- ✅ Server-driven UI
- ✅ A/B testing (different data)

**How AvaCode Stays Compliant**:
```
Plugin Author's Desktop:
  ↓
  Writes: WeatherPlugin.kt (AvaCode DSL)
  ↓
  Compiles: $ avacode compile WeatherPlugin.kt
  ↓
  Generates: plugin_weather.avaui (DATA, not executable code)
  ↓
  Uploads to: Avanues Plugin Server
  ↓
  User's Phone:
    ↓
    Downloads: plugin_weather.avaui (just data)
    ↓
    Parses: AvaUI runtime reads data structure
    ↓
    Interprets: Renders UI based on data (NOT executes code)
    ↓
    Actions: Only predefined types (http, database, navigate, etc.)
```

**Key Distinction**:
```
❌ Code Execution: Download → Execute → Run arbitrary logic
✅ Data Interpretation: Download → Parse → Render predefined UI components
```

**Apple's Rule**: "Apps may not download executable code" → AvaUI downloads DATA, not code.

---

### Section 2: Predefined Action Whitelist

**CONSTITUTIONAL RULE #7**: Plugins MAY only use predefined action types from the whitelist. No arbitrary logic.

**Approved Action Types** (Finite Set):

1. **http** - HTTP requests
   ```yaml
   actions:
     fetchData:
       type: http
       method: GET  # GET, POST, PUT, DELETE, PATCH
       url: "https://api.example.com/data"
       headers:
         Authorization: "Bearer {{token}}"
       responseMapping:
         result: "$.data.value"
   ```

2. **database** - Local SQLite operations
   ```yaml
   actions:
     saveNote:
       type: database
       operation: insert  # insert, update, delete, query
       table: "notes"
       data:
         title: "{{noteTitle}}"
         content: "{{noteContent}}"
   ```

3. **navigate** - Screen navigation
   ```yaml
   actions:
     goToDetails:
       type: navigate
       destination: "details"
       params:
         itemId: "{{selectedId}}"
   ```

4. **showToast** - UI feedback
   ```yaml
   actions:
     showSuccess:
       type: showToast
       message: "Saved successfully"
       duration: 3000  # milliseconds
   ```

5. **math** - Mathematical expressions (safe eval)
   ```yaml
   actions:
     calculateTip:
       type: math
       expression: "{{billAmount}} * {{tipPercent}} / 100"
       output: "tipAmount"
   ```

6. **conditional** - If/else logic
   ```yaml
   actions:
     checkAge:
       type: conditional
       condition: "{{age}} >= 18"
       then:
         action: "showAdultContent"
       else:
         action: "showKidContent"
   ```

7. **loop** - Iteration (map, filter, reduce)
   ```yaml
   actions:
     filterItems:
       type: loop
       operation: filter  # map, filter, reduce, forEach
       array: "{{items}}"
       condition: "item.price < 100"
       output: "filteredItems"
   ```

8. **intent** - Platform intents (Android)
   ```yaml
   actions:
     shareText:
       type: intent
       action: "android.intent.action.SEND"
       extras:
         "android.intent.extra.TEXT": "{{shareText}}"
   ```

9. **urlScheme** - URL schemes (iOS)
   ```yaml
   actions:
     openMaps:
       type: urlScheme
       url: "maps://?q={{address}}"
   ```

10. **storage** - Key-value storage
    ```yaml
    actions:
      savePreference:
        type: storage
        operation: set  # set, get, delete
        key: "user_theme"
        value: "{{selectedTheme}}"
    ```

11. **speech** - Voice input/output
    ```yaml
    actions:
      speakText:
        type: speech
        operation: speak  # speak, recognize
        text: "{{message}}"
    ```

12. **location** - GPS location
    ```yaml
    actions:
      getCurrentLocation:
        type: location
        operation: getCurrent  # getCurrent, watchPosition
        output: "currentLocation"
    ```

**ENFORCEMENT**: Any action type NOT on this whitelist MUST be rejected at parse time.

**Adding New Action Types**: Requires constitutional amendment (community vote + maintainer approval).

---

### Section 3: Plugin Sandbox

**CONSTITUTIONAL RULE #8**: Each plugin SHALL run in an isolated sandbox with declared permissions.

**Plugin Manifest** (Permissions):
```kotlin
@AvanuesPlugin(
    id = "com.user.weather",
    permissions = [
        "internet",           // Can make HTTP requests
        "location"           // Can access GPS
    ]
    // NOT requesting: "contacts", "storage", "camera"
)
```

**Sandbox Enforcement**:
```kotlin
class PluginSandbox(val plugin: Plugin) {
    private val isolatedStorage = File(context.filesDir, "plugins/${plugin.id}")
    private val isolatedDatabase = Room.databaseBuilder(
        context,
        PluginDatabase::class.java,
        "plugin_${plugin.id}.db"
    ).build()

    fun executeAction(action: Action) {
        // Permission check
        when (action.type) {
            "http" -> {
                if (!plugin.permissions.contains("internet")) {
                    throw SecurityException("Plugin ${plugin.id} missing 'internet' permission")
                }
            }
            "location" -> {
                if (!plugin.permissions.contains("location")) {
                    throw SecurityException("Plugin ${plugin.id} missing 'location' permission")
                }
            }
        }

        // Execute in isolated context
        withContext(PluginContext(plugin, isolatedStorage, isolatedDatabase)) {
            when (action.type) {
                "http" -> executeHttpAction(action)
                "database" -> executeDatabaseAction(action, isolatedDatabase)
                "storage" -> executeStorageAction(action, isolatedStorage)
                else -> throw IllegalArgumentException("Unknown action: ${action.type}")
            }
        }
    }
}
```

**Isolation Guarantees**:
- ✅ Each plugin has its own storage directory
- ✅ Each plugin has its own SQLite database
- ✅ Plugins CANNOT access other plugins' data
- ✅ Plugins CANNOT access Avanues's data
- ✅ Plugins CANNOT access device storage (except with "storage" permission)

---

## Article V: Performance & Quality Standards

### Section 1: Performance Targets

**CONSTITUTIONAL RULE #9**: AvaUI MUST meet the following performance targets:

| Metric | Target | Measurement |
|--------|--------|-------------|
| **UI Update Latency** | <1ms (99th percentile) | Profiler (Android/iOS) |
| **Frame Rate** | 60 FPS minimum | Frame stats |
| **Memory Overhead** | <5MB | Memory profiler |
| **App Size Increase** | <3MB compressed | APK/IPA size |
| **Compile Time** | <30s for 10K LOC | Build time |
| **Cold Start** | <100ms overhead | Startup profiler |

**ENFORCEMENT**: Any PR that degrades performance by >10% MUST be rejected.

**Optimization Techniques** (Required):
1. **Inline Functions** - Zero lambda allocations
2. **Value Classes** - 50% memory reduction
3. **Immutable Data** - Structural sharing
4. **Lazy Composition** - Defer rendering until visible
5. **Compile-Time Codegen** - 0% runtime overhead

---

### Section 2: Code Quality Standards

**Test Coverage**: 80% minimum (measured by JaCoCo)
**Documentation**: KDoc for all public APIs
**Null Safety**: Zero `!!` operators in production code
**Accessibility**: WCAG 2.1 AA compliance (all components)

---

## Article VI: Versioning & Compatibility

### Section 1: Semantic Versioning

**Format**: `MAJOR.MINOR.PATCH` (e.g., `1.2.3`)

- **MAJOR**: Breaking changes (increment when API changes)
- **MINOR**: New features (backwards compatible)
- **PATCH**: Bug fixes (backwards compatible)

**Examples**:
- `1.0.0` → `1.0.1`: Bug fix (patch)
- `1.0.1` → `1.1.0`: New components (minor)
- `1.1.0` → `2.0.0`: Renamed API (major)

**Deprecation Policy**:
1. Mark as `@Deprecated` in version N
2. Keep working for 2 minor versions (N, N+1, N+2)
3. Remove in version N+3

**Example**:
```kotlin
// v1.0.0: Original API
fun createButton(text: String)

// v1.1.0: New API, deprecate old
@Deprecated("Use Btn() instead", ReplaceWith("Btn(text)"))
fun createButton(text: String)

fun Btn(text: String)  // New API

// v1.2.0: Still works (deprecated)
// v1.3.0: Still works (deprecated)
// v2.0.0: REMOVED (breaking change)
```

---

## Article VII: Governance & Amendments

### Section 1: Authority

**Benevolent Dictator**: Manoj Jhawar (manoj@ideahq.net)
- Final decision on all constitutional matters
- Can veto any proposal
- Can amend constitution unilaterally

**Advisory Board** (Future):
- 5 community members
- 2 enterprise customers
- 1 security expert
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

**Approval Criteria**:
- Technical merit
- Community support (>50% positive feedback)
- Backwards compatibility (prefer non-breaking)
- Security implications

**Examples of Valid Amendments**:
- Adding new action type to whitelist
- Changing performance targets
- New format support (e.g., Protobuf)
- New encryption method

**Examples of Invalid Amendments**:
- Separating AvaUI and AvaCode (violates Article I)
- Removing encryption requirement (violates Article III)
- Allowing arbitrary code execution (violates Article IV)

---

## Article VIII: Intellectual Property & Licensing

### Section 1: License Tiers

**Free Tier** (Apache 2.0):
- `avaui-core` - Open source
- Free forever
- Commercial use allowed
- Attribution required

**Pro Tier** (Proprietary):
- `avaui-pro` - Closed source, encrypted
- $199/year per developer
- Commercial license
- No redistribution

**Enterprise Tier** (Proprietary):
- `avaui-enterprise` - Source access
- $2,999/year per company
- Custom license terms
- White-label allowed

**Plugin SDK**:
- Free SDK: Apache 2.0 (limited APIs)
- Pro SDK: Proprietary, encrypted
- Enterprise SDK: Source access

---

### Section 2: Trademark

**"AvaUI"** is a registered trademark of Augmentalis LLC.

**Allowed Uses**:
- ✅ "Built with AvaUI"
- ✅ "Compatible with AvaUI"
- ✅ "AvaUI Plugin"

**Prohibited Uses**:
- ❌ "AvaUI Pro" (implies official product)
- ❌ "AvaUI Enterprise" (trademark infringement)
- ❌ "AvaUI.com" (domain squatting)

---

## Article IX: Security & Privacy

### Section 1: Data Collection

**AvaUI SDK**:
- ✅ Collects: Error reports (opt-in), performance metrics (opt-in)
- ❌ Does NOT collect: Personal data, user input, location

**Plugin SDK**:
- ✅ Collects: License validation (required), usage stats (opt-in)
- ❌ Does NOT collect: Plugin source code, user data

**License Server**:
- ✅ Stores: License key, package name, expiration date
- ❌ Does NOT store: User's code, email (unless user provides)

**GDPR Compliance**:
- Right to deletion (delete license data)
- Right to access (export license data)
- No third-party sharing (data stays internal)

---

### Section 2: Vulnerability Disclosure

**Security Issues**: Email security@augmentalis.com
**Response Time**: 48 hours acknowledgment, 30 days fix
**Bounty Program**: $100-$10,000 depending on severity

---

## Article X: Ratification

This Constitution is hereby ratified and takes effect immediately.

**Effective Date**: 2025-11-01 11:55 PST

**Signatories**:
- Manoj Jhawar, Founder & CEO, Augmentalis LLC

**Witnesses**:
- Claude (AI Assistant, Anthropic) - Technical Advisor

---

## Appendix A: Quick Reference

### Format Preference Order
1. **AvaCode DSL** (Primary) - 96% less code, type-safe
2. **YAML** (Secondary) - Human-readable, server-driven
3. **Compact JSON** (Tertiary) - REST APIs, databases

### Predefined Action Types (12)
1. http
2. database
3. navigate
4. showToast
5. math
6. conditional
7. loop
8. intent (Android)
9. urlScheme (iOS)
10. storage
11. speech
12. location

### Performance Targets
- UI Update: <1ms
- Frame Rate: 60 FPS
- Memory: <5MB
- App Size: <3MB
- Compile: <30s/10K LOC

### License Tiers
- Free: Apache 2.0
- Pro: $199/year (encrypted)
- Enterprise: $2,999/year (source access)

### Security Layers
1. ProGuard (R8) - Low protection
2. String Encryption (DexGuard) - Medium
3. Class Encryption (DexGuard) - High
4. License Validation - Very High
5. Legal (EULA) - Ultimate

---

## Appendix B: Enforcement

**Violations of this Constitution**:
- **Code Review**: All PRs MUST comply (checked by CI/CD)
- **Documentation**: Must reference constitutional rules
- **Community**: Can report violations via GitHub issues
- **Sanctions**: Non-compliant code will be rejected

**Compliance Checklist** (CI/CD):
- [ ] Article I: Single package (no AvaCode separation)
- [ ] Article II: DSL examples in docs (not just YAML)
- [ ] Article III: SDK encrypted (DexGuard enabled)
- [ ] Article IV: No code execution (only data interpretation)
- [ ] Article V: Performance tests pass (<1ms, 60 FPS)
- [ ] Article VI: Semantic versioning followed
- [ ] Article VIII: License headers present

---

**Document Status**: ACTIVE - All implementations MUST comply
**Next Review**: 2026-11-01 (annual review)

**Created by Manoj Jhawar, manoj@ideahq.net**
**IDEACODE v5.0 - Constitutional Authority Established**
