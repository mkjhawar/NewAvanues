# MainAvanues Documentation Structure - Final Design
**Date:** 2025-11-24
**Purpose:** Comprehensive documentation organization with registry and naming conventions
**Approach:** Hybrid Centralized with Platform-First organization

---

## 🎯 Core Principles

1. **Centralized in `/docs`** - ALL documentation lives in `/docs` (not scattered in code folders)
2. **Mirror Platform Structure** - `/docs` mirrors `/android`, `/ios`, `/web`, `/common` structure
3. **Strict Naming Convention** - Universal file naming pattern enforced
4. **Registry-Based** - AI must check registry before creating files/folders
5. **Multiple IDEACODE Folders** - One at root (monorepo), one per app (app-specific)

---

## 📁 Complete Documentation Structure

```
MainAvanues/
├── docs/                                      ← ALL documentation here
│   ├── README.md                              ← Monorepo overview
│   ├── ARCHITECTURE.md                        ← Overall system design
│   ├── SETUP.md                               ← Development setup
│   ├── CONTRIBUTING.md                        ← How to contribute
│   ├── TESTING.md                             ← Test strategy
│   ├── DEPLOYMENT.md                          ← Deployment guides
│   │
│   ├── ideacode/                              ← Root IDEACODE (monorepo-level)
│   │   ├── specs/                             ← Cross-cutting specs
│   │   ├── features/                          ← Monorepo-wide features
│   │   ├── archive/                           ← Completed monorepo features
│   │   ├── registries/                        ← Master registries
│   │   │   ├── DOCUMENTATION-REGISTRY.json    ← Registry of all doc locations
│   │   │   ├── API-REGISTRY.json              ← All APIs
│   │   │   ├── IPC-REGISTRY.json              ← All IPC methods
│   │   │   └── INTENT-REGISTRY.json           ← All Intents
│   │   └── protocols/                         ← Development protocols
│   │
│   ├── android/                               ← Android documentation
│   │   ├── apps/                              ← Android app docs
│   │   │   ├── ava/
│   │   │   │   ├── README.md
│   │   │   │   ├── dev-architecture.md
│   │   │   │   ├── dev-setup.md
│   │   │   │   ├── user-guide.md
│   │   │   │   ├── api-endpoints.md
│   │   │   │   └── ideacode/                  ← App-level IDEACODE
│   │   │   │       ├── specs/
│   │   │   │       ├── features/
│   │   │   │       └── registries/
│   │   │   │
│   │   │   ├── voiceos/
│   │   │   │   ├── README.md
│   │   │   │   ├── dev-accessibility-service.md
│   │   │   │   └── ideacode/
│   │   │   │
│   │   │   ├── avaconnect/
│   │   │   │   └── ideacode/
│   │   │   │
│   │   │   └── avanues/
│   │   │       └── ideacode/
│   │   │
│   │   └── modules/                           ← Android module docs
│   │       ├── accessibility-service/
│   │       │   └── README.md
│   │       └── system-integration/
│   │           └── README.md
│   │
│   ├── ios/                                   ← iOS documentation (future)
│   │   └── apps/
│   │       └── ava/
│   │           ├── README.md
│   │           └── ideacode/
│   │
│   ├── web/                                   ← Web documentation
│   │   └── apps/
│   │       └── webavanue/
│   │           ├── README.md
│   │           ├── dev-architecture.md
│   │           └── ideacode/
│   │
│   ├── common/                                ← Common/shared library docs
│   │   └── libs/
│   │       ├── voice/
│   │       │   ├── feature-recognition/
│   │       │   │   ├── README.md
│   │       │   │   ├── api-recognition.md
│   │       │   │   ├── dev-platform-differences.md
│   │       │   │   └── user-voice-commands.md
│   │       │   │
│   │       │   └── data-access-api/
│   │       │       └── README.md
│   │       │
│   │       ├── accessibility/
│   │       │   └── feature-voice-cursor/
│   │       │       └── README.md
│   │       │
│   │       ├── cloud/
│   │       │   └── feature-sync/
│   │       │       └── README.md
│   │       │
│   │       └── shared/
│   │           ├── ui-design-system/
│   │           │   ├── README.md
│   │           │   └── api-components.md
│   │           └── util-network/
│   │               └── README.md
│   │
│   ├── examples/                              ← Code examples
│   │   ├── voice-commands/
│   │   └── accessibility-integration/
│   │
│   └── archive/                               ← Old/deprecated docs
│       └── 2024/
│
├── android/                                   ← Code only (NO docs)
│   └── apps/
│       └── ava/
│           └── src/
│
├── common/                                    ← Code only (NO docs)
│   └── libs/
│       └── voice/
│           └── feature-recognition/
│               └── src/
│
└── [other platform folders...]
```

---

## 📝 Universal File Naming Convention

### **Format:** `{type}-{context}-{topic}.md`

### **Types (Prefix):**
- `dev-` - Developer documentation
- `api-` - API documentation
- `user-` - User documentation
- `spec-` - Specification (IDEACODE)
- `adr-` - Architecture Decision Record
- `rfc-` - Request for Comments
- `test-` - Test plan/report
- `fix-` - Bug fix documentation

### **Context (Middle):**
- Specific feature, module, or area
- Examples: `authentication`, `voice-recognition`, `accessibility`

### **Topic (Suffix):**
- What aspect is documented
- Examples: `setup`, `architecture`, `endpoints`, `guide`

### **Examples:**

| File | Location | Description |
|------|----------|-------------|
| `README.md` | All folders | Overview (special case, no prefix) |
| `dev-architecture.md` | `docs/android/apps/ava/` | Developer: AVA architecture |
| `dev-setup.md` | `docs/android/apps/ava/` | Developer: How to set up AVA |
| `dev-accessibility-service.md` | `docs/android/apps/voiceos/` | Developer: Accessibility service guide |
| `api-endpoints.md` | `docs/android/apps/ava/` | API: REST endpoints |
| `api-recognition.md` | `docs/common/libs/voice/feature-recognition/` | API: Voice recognition API |
| `api-components.md` | `docs/common/libs/shared/ui-design-system/` | API: UI component API |
| `user-guide.md` | `docs/android/apps/ava/` | User: End-user guide |
| `user-voice-commands.md` | `docs/common/libs/voice/feature-recognition/` | User: Voice command reference |
| `spec-voice-dsl.md` | `docs/ideacode/specs/` | Spec: Voice DSL specification |
| `spec-auth-module.md` | `docs/android/apps/ava/ideacode/specs/` | Spec: AVA auth module |
| `dev-platform-differences.md` | `docs/common/libs/voice/feature-recognition/` | Developer: Platform quirks |
| `test-voice-recognition.md` | `docs/common/libs/voice/feature-recognition/` | Test: Test plan |
| `adr-kmp-adoption.md` | `docs/archive/2024/` | ADR: Historical decision |

---

## 📋 Documentation Registry

### **Master Registry: `docs/ideacode/registries/DOCUMENTATION-REGISTRY.json`**

```json
{
  "version": "1.0.0",
  "last_updated": "2025-11-24",
  "description": "Registry of all documentation locations in MainAvanues monorepo",

  "structure": {
    "root_docs": "/docs",
    "platforms": ["android", "ios", "web", "common"],
    "ideacode_locations": [
      "/docs/ideacode",
      "/docs/android/apps/ava/ideacode",
      "/docs/android/apps/voiceos/ideacode",
      "/docs/android/apps/avaconnect/ideacode",
      "/docs/android/apps/avanues/ideacode"
    ]
  },

  "naming_convention": {
    "format": "{type}-{context}-{topic}.md",
    "types": ["dev", "api", "user", "spec", "adr", "rfc", "test", "fix"],
    "required_files": ["README.md"]
  },

  "app_docs": [
    {
      "app": "ava",
      "platform": "android",
      "path": "/docs/android/apps/ava",
      "ideacode_path": "/docs/android/apps/ava/ideacode",
      "required_files": [
        "README.md",
        "dev-architecture.md",
        "dev-setup.md"
      ]
    },
    {
      "app": "voiceos",
      "platform": "android",
      "path": "/docs/android/apps/voiceos",
      "ideacode_path": "/docs/android/apps/voiceos/ideacode"
    },
    {
      "app": "avaconnect",
      "platform": "android",
      "path": "/docs/android/apps/avaconnect",
      "ideacode_path": "/docs/android/apps/avaconnect/ideacode"
    },
    {
      "app": "avanues",
      "platform": "android",
      "path": "/docs/android/apps/avanues",
      "ideacode_path": "/docs/android/apps/avanues/ideacode"
    },
    {
      "app": "webavanue",
      "platform": "web",
      "path": "/docs/web/apps/webavanue",
      "ideacode_path": "/docs/web/apps/webavanue/ideacode"
    }
  ],

  "library_docs": [
    {
      "library": "voice/feature-recognition",
      "path": "/docs/common/libs/voice/feature-recognition",
      "required_files": [
        "README.md",
        "api-recognition.md",
        "dev-platform-differences.md"
      ]
    },
    {
      "library": "voice/data-access-api",
      "path": "/docs/common/libs/voice/data-access-api"
    },
    {
      "library": "accessibility/feature-voice-cursor",
      "path": "/docs/common/libs/accessibility/feature-voice-cursor"
    },
    {
      "library": "cloud/feature-sync",
      "path": "/docs/common/libs/cloud/feature-sync"
    },
    {
      "library": "shared/ui-design-system",
      "path": "/docs/common/libs/shared/ui-design-system",
      "required_files": [
        "README.md",
        "api-components.md"
      ]
    }
  ],

  "rules": {
    "check_registry_before_create": true,
    "enforce_naming_convention": true,
    "require_readme": true,
    "max_nesting_depth": 5,
    "allowed_extensions": [".md", ".json", ".yaml", ".png", ".jpg", ".svg"]
  }
}
```

---

## 🤖 AI Usage Protocol

### **Before Creating Any Documentation:**

1. **Check Registry**
   ```
   Read: docs/ideacode/registries/DOCUMENTATION-REGISTRY.json
   ```

2. **Determine Correct Path**
   - App doc? → `docs/{platform}/apps/{app}/`
   - Library doc? → `docs/common/libs/{scope}/{library}/`
   - Monorepo doc? → `docs/`
   - IDEACODE spec? → Check which level (root or app)

3. **Validate Naming**
   - Use correct prefix: `dev-`, `api-`, `user-`, `spec-`, etc.
   - Format: `{type}-{context}-{topic}.md`

4. **Create File**
   - Place in registry-specified location
   - Follow naming convention

5. **Update Registry** (if new app/library)
   - Add entry to `DOCUMENTATION-REGISTRY.json`

---

## 🔧 IDEACODE Multi-Location Strategy

### **Two-Tier IDEACODE System:**

#### **Tier 1: Root IDEACODE (Monorepo-Level)**
**Location:** `/docs/ideacode/`

**Purpose:** Cross-cutting concerns, monorepo-wide features

**Contents:**
- `specs/` - Platform-agnostic specs (Voice DSL, Architecture patterns)
- `features/` - Monorepo-wide features
- `registries/` - Master registries (API, IPC, Intent, Documentation)
- `protocols/` - Development protocols
- `archive/` - Completed monorepo features

**Examples:**
- `specs/spec-voice-dsl-v2.md` - Voice DSL specification (applies to all apps)
- `specs/spec-kmp-architecture.md` - KMP architecture (monorepo-wide)
- `features/feature-monorepo-migration.md` - Migration from multi-repo

#### **Tier 2: App-Level IDEACODE**
**Location:** `/docs/{platform}/apps/{app}/ideacode/`

**Purpose:** App-specific features, specs

**Contents:**
- `specs/` - App-specific specs
- `features/` - Features for this app only
- `archive/` - Completed app features

**Examples:**
- `docs/android/apps/ava/ideacode/specs/spec-auth-module.md`
- `docs/android/apps/voiceos/ideacode/specs/spec-accessibility-gestures.md`
- `docs/web/apps/webavanue/ideacode/specs/spec-browser-extension-api.md`

### **Decision Tree: Which IDEACODE Folder?**

```
Does this spec/feature apply to multiple apps or the entire monorepo?
├─ YES → Root IDEACODE
│         /docs/ideacode/specs/
│         Example: Voice DSL, KMP Architecture, Monorepo structure
│
└─ NO → App-Level IDEACODE
          /docs/{platform}/apps/{app}/ideacode/specs/
          Example: AVA auth module, VoiceOS gestures, WebAvanue extension API
```

---

## 📊 Comparison with Current Chaos

### **Before (Current MainAvanues):**
```
❌ Scattered everywhere:
├── docs/
│   ├── developer/
│   │   └── chapters/
│   ├── specs/
│   │   ├── architecture/
│   │   ├── backlog/
│   │   ├── features/
│   │   └── requirements/
│   ├── manuals/
│   │   ├── design/
│   │   ├── developer/
│   │   └── user/
│   ├── ideacode/
│   │   ├── specs/
│   │   ├── features/
│   │   ├── memory/         ← Temporary
│   │   ├── session-summaries/  ← Temporary
│   │   └── backups/        ← Temporary
│   ├── bugs/
│   ├── changelogs/
│   ├── decisions/
│   └── demos/

Total: 20+ folders, unclear organization
```

### **After (Proposed):**
```
✅ Clear hierarchy:
docs/
├── {root-level-guides}.md           ← Flat monorepo docs
├── ideacode/                         ← Root IDEACODE (monorepo)
├── android/apps/{app}/               ← Android app docs
│   └── ideacode/                     ← App-level IDEACODE
├── ios/apps/{app}/                   ← iOS app docs
├── web/apps/{app}/                   ← Web app docs
├── common/libs/{scope}/{lib}/        ← Library docs
├── examples/                         ← Examples
└── archive/                          ← Old docs by year

Total: 7 top-level folders, clear purpose, strict naming
```

---

## ✅ Enforcement Checklist

### **IDEACODE MCP Tool Updates Needed:**

- [ ] `ideacode_specify` - Check registry for doc path before creating spec
- [ ] `ideacode_plan` - Validate naming convention
- [ ] `ideacode_implement` - Update registry when creating new app/lib
- [ ] `ideacode_archive` - Move completed features to correct ideacode/archive
- [ ] Add `ideacode_validate_docs` - Validate docs against registry
- [ ] Add `ideacode_check_registry` - Query registry for correct path

### **Pre-Commit Hook:**

```bash
#!/bin/bash
# Enforce documentation structure

# Check if any doc files were added/modified
DOC_FILES=$(git diff --cached --name-only --diff-filter=AM | grep '^docs/')

if [ -n "$DOC_FILES" ]; then
    echo "Validating documentation structure..."

    # Check naming convention
    for file in $DOC_FILES; do
        basename=$(basename "$file")
        if [[ ! "$basename" =~ ^(dev|api|user|spec|adr|rfc|test|fix)-.+\.md$ ]] && \
           [[ "$basename" != "README.md" ]]; then
            echo "ERROR: Invalid file name: $basename"
            echo "Must follow: {type}-{context}-{topic}.md"
            echo "Types: dev, api, user, spec, adr, rfc, test, fix"
            exit 1
        fi
    done

    echo "✅ Documentation structure valid"
fi
```

### **AI Prompt Template:**

```markdown
Before creating any documentation file, I must:

1. Check: /docs/ideacode/registries/DOCUMENTATION-REGISTRY.json
2. Determine path:
   - App doc? → /docs/{platform}/apps/{app}/
   - Library doc? → /docs/common/libs/{scope}/{library}/
   - Root doc? → /docs/
3. Validate naming: {type}-{context}-{topic}.md
4. Create file in correct location
5. Update registry if new app/library

Naming types: dev, api, user, spec, adr, rfc, test, fix
```

---

## 📋 Migration Checklist

### **Phase 1: Create New Structure**
- [ ] Create `/docs` structure matching platform folders
- [ ] Create `DOCUMENTATION-REGISTRY.json`
- [ ] Create root IDEACODE folder
- [ ] Create app-level IDEACODE folders

### **Phase 2: Migrate Existing Docs**
- [ ] Map current docs to new locations
- [ ] Rename files to follow convention
- [ ] Move to correct paths
- [ ] Delete temporary folders (`memory/`, `backups/`, etc.)

### **Phase 3: Update Tools**
- [ ] Update IDEACODE MCP tools to use registry
- [ ] Add pre-commit hook
- [ ] Update `.claude/CLAUDE.md` with new structure
- [ ] Create migration guide

### **Phase 4: Enforce**
- [ ] Train team on new structure
- [ ] Monitor for violations
- [ ] Refine registry as needed

---

## 🎓 Examples of Real Usage

### **Example 1: Creating AVA Authentication Spec**

```bash
# AI checks registry
registry = read("docs/ideacode/registries/DOCUMENTATION-REGISTRY.json")

# Question: Monorepo-wide or app-specific?
# Answer: App-specific (only AVA needs this)

# Path from registry
path = "docs/android/apps/ava/ideacode/specs/"

# Naming convention
filename = "spec-auth-module.md"

# Create
write("docs/android/apps/ava/ideacode/specs/spec-auth-module.md")
```

### **Example 2: Creating Voice Recognition Library API Docs**

```bash
# Check registry
registry.library_docs["voice/feature-recognition"]
# Path: "docs/common/libs/voice/feature-recognition"

# Naming convention
filename = "api-recognition.md"

# Create
write("docs/common/libs/voice/feature-recognition/api-recognition.md")
```

### **Example 3: Creating Voice DSL Spec (Monorepo-Wide)**

```bash
# Question: Monorepo-wide or app-specific?
# Answer: Monorepo-wide (applies to all voice-enabled apps)

# Path from registry
path = "docs/ideacode/specs/"

# Naming convention
filename = "spec-voice-dsl-v2.md"

# Create
write("docs/ideacode/specs/spec-voice-dsl-v2.md")
```

---

## ✨ Key Benefits

1. **Single Source of Truth** - All docs in `/docs`
2. **Clear Organization** - Mirrors platform structure
3. **Easy Discovery** - Know exactly where to look
4. **Strict Enforcement** - Registry + naming convention
5. **Multi-Level IDEACODE** - Root for monorepo, app-level for apps
6. **AI-Friendly** - Registry makes AI decisions deterministic
7. **Scalable** - Add new apps/libs, just update registry

---

**Next:** Create migration script to move existing docs to new structure
