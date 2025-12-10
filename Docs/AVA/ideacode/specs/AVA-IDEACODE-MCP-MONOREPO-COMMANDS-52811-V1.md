# IDEACODE-MCP Monorepo Commands Specification

**Version:** 1.0.0
**Date:** 2025-11-28
**Purpose:** Specification for ideacode-mcp to implement monorepo folder validation and enforcement
**Target:** MainAvanues monorepo (VoiceOS, AVA, WebAvanue, AVAConnect consolidation)

---

## 📋 Overview

This specification defines new MCP commands for the `ideacode-mcp` server to enforce strict folder structure and naming conventions in the MainAvanues monorepo.

**Problem:**
- Multiple repositories being consolidated into one monorepo
- Need to prevent folder duplication/variation across sessions
- Need to enforce naming conventions (kebab-case, no type prefixes)
- Need single source of truth for folder structure

**Solution:**
- Centralized FOLDER-REGISTRY.md (master folder registry)
- MCP commands to validate, create, and check folders
- Automatic enforcement before folder creation
- Clear error messages and suggestions

---

## 🎯 Commands to Implement

### 1. `ideacode_validate_folder`

**Purpose:** Check if a folder path is valid according to the registry and naming conventions

**Signature:**
```typescript
async function ideacode_validate_folder(args: {
  folder_path: string;
  project_root?: string;  // Optional, defaults to current working directory
}): Promise<{
  status: 'valid' | 'warning' | 'error' | 'not_in_registry';
  message: string;
  suggestion?: string;
  existing_folder?: string;  // If variation detected, show correct folder
}>
```

**Behavior:**

1. **Read FOLDER-REGISTRY.md** from project root
2. **Parse folder path** to determine:
   - Top-level category (android/, ios/, desktop/, common/, docs/, scripts/, etc.)
   - Sub-path within category
3. **Check naming convention:**
   - Is it kebab-case? (lowercase-with-dashes)
   - Does it have type prefixes? (feature-, spec-, etc.)
   - Does it violate any naming rules?
4. **Check registry:**
   - Does exact folder exist in registry?
   - Is it a variation of an existing folder?
   - Does it follow the pattern for its location?
5. **Return result:**
   - ✅ `valid` - Folder matches registry exactly
   - ⚠️ `warning` - Folder valid but not in registry (new folder)
   - ⚠️ `warning` - Folder is close variation (suggest correct name)
   - ❌ `error` - Folder violates naming convention
   - ℹ️ `not_in_registry` - Folder not in registry but follows conventions

**Example Usage:**
```bash
# Valid folder
ideacode_validate_folder "android/voiceos"
# ✅ Status: valid
# Message: Folder 'android/voiceos' exists in registry and follows conventions

# Variation detected
ideacode_validate_folder "android/voice-os"
# ⚠️ Status: warning
# Message: Folder 'android/voice-os' is a variation of 'android/voiceos'
# Suggestion: Use 'android/voiceos' instead

# Naming violation
ideacode_validate_folder "android/VoiceOS"
# ❌ Status: error
# Message: Folder 'android/VoiceOS' violates naming convention (must be kebab-case)
# Suggestion: Use 'android/voiceos' instead

# Not in registry (but valid)
ideacode_validate_folder "common/new-module"
# ℹ️ Status: not_in_registry
# Message: Folder 'common/new-module' not in registry but follows conventions
# Suggestion: Create folder and update FOLDER-REGISTRY.md
```

---

### 2. `ideacode_create_folder`

**Purpose:** Create a folder with automatic validation and registry update

**Signature:**
```typescript
async function ideacode_create_folder(args: {
  folder_path: string;
  purpose: string;  // Description of what the folder is for
  force?: boolean;  // Skip validation (dangerous, requires confirmation)
  project_root?: string;
}): Promise<{
  status: 'created' | 'exists' | 'error';
  message: string;
  validation_result?: ValidationResult;
}>
```

**Behavior:**

1. **Validate first** using `ideacode_validate_folder`
2. **If validation fails:**
   - Show error message
   - Suggest correct folder name
   - Return error status
3. **If folder exists:**
   - Return 'exists' status
   - Don't create duplicate
4. **If validation passes:**
   - Create folder
   - Add entry to FOLDER-REGISTRY.md (if not already present)
   - Return success status
5. **If force=true:**
   - Warn user about skipping validation
   - Create folder anyway
   - Update registry

**Example Usage:**
```bash
# Create valid folder
ideacode_create_folder "common/auth" --purpose "Authentication module"
# ✅ Status: created
# Message: Created 'common/auth' and updated FOLDER-REGISTRY.md

# Try to create invalid folder
ideacode_create_folder "common/feature-auth" --purpose "Auth feature"
# ❌ Status: error
# Message: Folder 'common/feature-auth' violates naming convention
# Suggestion: Remove 'feature-' prefix. Use 'common/auth' instead

# Force create (dangerous)
ideacode_create_folder "common/AuthModule" --purpose "Auth" --force true
# ⚠️ Status: created
# Message: WARNING: Created 'common/AuthModule' with validation bypass
# Recommendation: Rename to 'common/auth-module' to follow conventions
```

---

### 3. `ideacode_check_naming`

**Purpose:** Check if a filename or folder name follows naming conventions

**Signature:**
```typescript
async function ideacode_check_naming(args: {
  name: string;
  type: 'folder' | 'file';
  context?: string;  // Where the file/folder will be placed
}): Promise<{
  valid: boolean;
  violations: string[];  // List of naming violations
  suggestion?: string;  // Corrected name
}>
```

**Behavior:**

1. **Check for naming convention violations:**
   - Not kebab-case (for folders and markdown files)
   - Has type prefix (feature-, spec-, data-, etc.)
   - Uses underscores instead of dashes
   - Uses PascalCase/camelCase for non-code files
   - Uses UPPERCASE incorrectly (exceptions: README, CHANGELOG, etc.)
2. **Return violations** with explanations
3. **Generate suggestion** with corrected name

**Example Usage:**
```bash
# Valid folder name
ideacode_check_naming "voice-recognition" --type folder
# ✅ Valid: true
# Violations: []

# Invalid folder name (type prefix)
ideacode_check_naming "feature-authentication" --type folder
# ❌ Valid: false
# Violations: ["Has type prefix 'feature-'"]
# Suggestion: "authentication"

# Invalid folder name (PascalCase)
ideacode_check_naming "VoiceRecognition" --type folder
# ❌ Valid: false
# Violations: ["Uses PascalCase instead of kebab-case"]
# Suggestion: "voice-recognition"

# Valid file name
ideacode_check_naming "voice-recognition-spec.md" --type file
# ✅ Valid: true
# Violations: []

# Invalid file name (type prefix)
ideacode_check_naming "spec-voice-recognition.md" --type file
# ❌ Valid: false
# Violations: ["Type prefix 'spec-' at beginning instead of end"]
# Suggestion: "voice-recognition-spec.md"
```

---

### 4. `ideacode_registry_search`

**Purpose:** Search FOLDER-REGISTRY.md for folders or files

**Signature:**
```typescript
async function ideacode_registry_search(args: {
  query: string;  // Search term (folder name, app name, module name)
  type?: 'folder' | 'file' | 'all';
  project_root?: string;
}): Promise<{
  results: Array<{
    path: string;
    purpose: string;
    category: string;  // Platform, Common, Docs, etc.
  }>;
  count: number;
}>
```

**Behavior:**

1. **Read FOLDER-REGISTRY.md**
2. **Search for query** in:
   - Folder paths
   - Folder purposes
   - File patterns
3. **Return matching entries** with context

**Example Usage:**
```bash
# Search for voice-related folders
ideacode_registry_search "voice"
# Results:
# - android/voiceos (VoiceOS Android app)
# - ios/voiceos (VoiceOS iOS app)
# - common/voice (Voice recognition/synthesis shared)
# - docs/voiceos/Master (VoiceOS universal specs)
# Count: 4

# Search for database folders
ideacode_registry_search "database"
# Results:
# - common/database (SQLDelight unified database)
# - docs/voiceos/Master/database-schema.md (VoiceOS database schema)
# Count: 2
```

---

### 5. `ideacode_registry_update`

**Purpose:** Update FOLDER-REGISTRY.md with new folder entry

**Signature:**
```typescript
async function ideacode_registry_update(args: {
  folder_path: string;
  purpose: string;
  category: string;  // Platform, Common, Docs, Scripts, etc.
  shared_by?: string[];  // For common/ modules: list of apps using it
  project_root?: string;
}): Promise<{
  status: 'updated' | 'already_exists' | 'error';
  message: string;
}>
```

**Behavior:**

1. **Read FOLDER-REGISTRY.md**
2. **Check if folder already in registry**
3. **If not present:**
   - Add entry to appropriate section (based on category)
   - Maintain alphabetical order within section
   - Include purpose and metadata
   - Update "Last Updated" timestamp
4. **If already present:**
   - Return 'already_exists' status
   - Don't duplicate

**Example Usage:**
```bash
# Add new common module
ideacode_registry_update \
  "common/auth" \
  --purpose "Authentication and authorization logic" \
  --category "Common" \
  --shared-by "voiceos,ava,webavanue"

# ✅ Status: updated
# Message: Added 'common/auth' to FOLDER-REGISTRY.md (Common section)
```

---

## 📂 Registry File Format

### FOLDER-REGISTRY.md Structure

The registry is organized in sections with markdown tables:

```markdown
# MainAvanues Monorepo - Folder Registry

## Root-Level Folders

| Folder | Purpose | Status | Naming Rule |
|--------|---------|--------|-------------|
| `android/` | Android platform apps | Required | Platform name |
| `ios/` | iOS platform apps | Required | Platform name |
...

## Platform Folders (android/, ios/, desktop/)

| Folder | Purpose | Apps |
|--------|---------|------|
| `android/voiceos/` | VoiceOS Android app | Voice OS for Android |
| `android/ava/` | AVA Android app | AI assistant for Android |
...

## Common Folder (KMP Shared Modules)

| Folder | Purpose | Shared By |
|--------|---------|-----------|
| `common/database/` | SQLDelight unified database | All apps (post-consolidation) |
| `common/voice/` | Voice recognition/synthesis | VoiceOS, AVA |
...

## Documentation Folders

### App-Level Documentation

| Folder | Purpose | Content |
|--------|---------|---------|
| `docs/voiceos/Master/` | VoiceOS universal specs | Architecture, vision, flows |
...
```

**Parsing Requirements:**

1. **Section headers** use `##` and `###` markdown
2. **Tables** use standard markdown table format
3. **Folder paths** are in backticks: `` `common/voice/` ``
4. **Extract metadata** from each row:
   - Folder path (column 1)
   - Purpose (column 2)
   - Additional info (columns 3+)

---

## 🔍 Validation Rules

### Folder Naming Conventions

| Rule | Valid | Invalid |
|------|-------|---------|
| **Kebab-case** | `voice-recognition` | `VoiceRecognition`, `voice_recognition` |
| **No type prefixes** | `authentication` | `feature-authentication`, `module-auth` |
| **No trailing slashes** | `common/voice` | `common/voice/` |
| **Platform exact match** | `android` | `Android`, `ANDROID` |
| **App exact match** | `voiceos` | `VoiceOS`, `voice-os` |
| **Lowercase** | `docs` | `Docs`, `DOCS` |

**Exceptions (UPPERCASE allowed):**
- `README.md`
- `CHANGELOG.md`
- `REGISTRY.md`
- `CLAUDE.md`
- `LICENSE`
- `FOLDER-REGISTRY.md`
- `PROJECT-INSTRUCTIONS.md`

### File Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| **Specs** | `{feature}-spec.md` | `voice-recognition-spec.md` |
| **Plans** | `{feature}-plan.md` | `database-consolidation-plan.md` |
| **Tasks** | `{feature}-tasks.md` | `monorepo-migration-tasks.md` |
| **Architecture** | `{component}-architecture.md` | `nlu-architecture.md` |
| **Timestamped** | `{name}-YYYYMMDDHHMM.md` | `migration-report-202511280930.md` |
| **Kotlin files** | `PascalCase.kt` | `VoiceManager.kt` |

---

## 🚨 Error Messages

### Clear, Actionable Error Messages

**Format:**
```
❌ Error: [Brief description]

Issue: [Detailed explanation]
Rule: [Which rule was violated]
Suggestion: [How to fix]

Example:
  ❌ common/feature-auth
  ✅ common/auth
```

**Examples:**

```
❌ Error: Invalid folder name 'android/VoiceOS'

Issue: Folder name uses PascalCase instead of lowercase
Rule: Platform app folders must be lowercase kebab-case
Suggestion: Use 'android/voiceos' instead

Example:
  ❌ android/VoiceOS
  ✅ android/voiceos
```

```
❌ Error: Type prefix detected in 'common/feature-authentication'

Issue: Folder name has 'feature-' prefix
Rule: No type prefixes allowed (feature-, module-, lib-, etc.)
Suggestion: Remove 'feature-' prefix. Use 'common/authentication'

Example:
  ❌ common/feature-authentication
  ✅ common/authentication
```

```
⚠️ Warning: Folder variation detected

Requested: 'android/voice-os'
Registry has: 'android/voiceos'

Did you mean 'android/voiceos'?

If creating new folder, it will cause duplication.
Use existing folder or provide justification for new folder.
```

---

## 🔧 Implementation Guide

### Language
**TypeScript** (for ideacode-mcp MCP server)

### Dependencies
- **fs/promises** - File system operations
- **path** - Path manipulation
- **marked** (optional) - Markdown parsing
- **@modelcontextprotocol/sdk** - MCP SDK

### File Locations
- **FOLDER-REGISTRY.md** - `{project_root}/FOLDER-REGISTRY.md`
- **PROJECT-INSTRUCTIONS.md** - `{project_root}/PROJECT-INSTRUCTIONS.md`

### Pseudo-code

```typescript
// ideacode_validate_folder implementation
async function ideacode_validate_folder(args) {
  // 1. Find project root (look for FOLDER-REGISTRY.md)
  const projectRoot = await findProjectRoot(args.project_root);

  // 2. Read FOLDER-REGISTRY.md
  const registryPath = path.join(projectRoot, 'FOLDER-REGISTRY.md');
  const registryContent = await fs.readFile(registryPath, 'utf-8');

  // 3. Parse registry (extract folder paths from tables)
  const registeredFolders = parseRegistry(registryContent);

  // 4. Check naming convention
  const namingResult = checkNamingConvention(args.folder_path);
  if (!namingResult.valid) {
    return {
      status: 'error',
      message: `Folder violates naming convention: ${namingResult.violations.join(', ')}`,
      suggestion: namingResult.suggestion
    };
  }

  // 5. Check if folder in registry
  const exactMatch = registeredFolders.find(f => f.path === args.folder_path);
  if (exactMatch) {
    return {
      status: 'valid',
      message: `Folder '${args.folder_path}' exists in registry`
    };
  }

  // 6. Check for variations (Levenshtein distance, case differences)
  const variation = findVariation(args.folder_path, registeredFolders);
  if (variation) {
    return {
      status: 'warning',
      message: `Folder '${args.folder_path}' is a variation of existing folder`,
      suggestion: `Use '${variation.path}' instead`,
      existing_folder: variation.path
    };
  }

  // 7. Not in registry but valid
  return {
    status: 'not_in_registry',
    message: `Folder '${args.folder_path}' not in registry but follows conventions`,
    suggestion: 'Create folder and update FOLDER-REGISTRY.md'
  };
}

// Helper: Parse FOLDER-REGISTRY.md tables
function parseRegistry(content: string): Array<{path: string, purpose: string}> {
  const folders = [];

  // Find all markdown tables
  const tableRegex = /\|[^\n]+\|/g;
  const tables = content.match(tableRegex);

  for (const row of tables) {
    // Extract folder path (in backticks)
    const pathMatch = row.match(/`([^`]+)`/);
    if (pathMatch) {
      const folderPath = pathMatch[1].replace(/\/$/, ''); // Remove trailing slash

      // Extract purpose (second column)
      const columns = row.split('|').map(c => c.trim()).filter(c => c);
      const purpose = columns[1] || '';

      folders.push({ path: folderPath, purpose });
    }
  }

  return folders;
}

// Helper: Check naming convention
function checkNamingConvention(folderPath: string) {
  const violations = [];
  const parts = folderPath.split('/');

  for (const part of parts) {
    // Skip empty parts
    if (!part) continue;

    // Check for type prefixes
    if (part.match(/^(feature|module|lib|spec|data|domain)-/)) {
      violations.push(`Type prefix detected in '${part}'`);
    }

    // Check for PascalCase/camelCase
    if (part.match(/[A-Z]/) && !isException(part)) {
      violations.push(`Uses uppercase in '${part}' (must be lowercase)`);
    }

    // Check for underscores (should use dashes)
    if (part.includes('_')) {
      violations.push(`Uses underscores in '${part}' (use dashes)`);
    }
  }

  return {
    valid: violations.length === 0,
    violations,
    suggestion: generateSuggestion(folderPath, violations)
  };
}

// Helper: Find variations using fuzzy matching
function findVariation(query: string, folders: Array<{path: string}>) {
  // Simple variation detection:
  // 1. Case-insensitive match
  const lowerQuery = query.toLowerCase();
  const caseMatch = folders.find(f => f.path.toLowerCase() === lowerQuery && f.path !== query);
  if (caseMatch) return caseMatch;

  // 2. Dash/underscore variation
  const dashQuery = query.replace(/_/g, '-');
  const dashMatch = folders.find(f => f.path === dashQuery);
  if (dashMatch) return dashMatch;

  // 3. Levenshtein distance < 3
  for (const folder of folders) {
    if (levenshteinDistance(query, folder.path) < 3) {
      return folder;
    }
  }

  return null;
}
```

---

## 🎯 Integration with IDEACODE Workflow

### Pre-Code Checklist Update

Add folder validation to IDEACODE pre-code checklist:

**Before creating any folder:**
```bash
1. Run: ideacode_validate_folder "{folder_path}"
2. If error: Fix naming and retry
3. If warning (variation): Use existing folder
4. If not_in_registry: Create and update registry
5. Never force-create without validation
```

### Claude Code Integration

**Automatic validation in Claude Code:**

```
When Claude Code attempts to create a folder:

1. Call ideacode_validate_folder first
2. If status = 'error': Show error, suggest fix, don't create
3. If status = 'warning': Show warning, ask user confirmation
4. If status = 'valid' or 'not_in_registry': Proceed with creation
5. After creation: Call ideacode_registry_update
```

---

## 📊 Success Metrics

### Enforcement Effectiveness

**Goals:**
- ✅ 100% folder validation before creation
- ✅ Zero folder duplicates/variations
- ✅ 100% compliance with kebab-case naming
- ✅ Zero type prefixes in folder names
- ✅ Registry always up-to-date

**Metrics to track:**
- Number of validation errors prevented
- Number of variations detected and corrected
- Number of folders created with validation
- Registry update frequency

---

## 🚀 Deployment

### MCP Server Update

1. **Implement commands** in `ideacode-mcp/src/tools/`
2. **Add to tool registry** in `ideacode-mcp/src/index.ts`
3. **Test with FOLDER-REGISTRY.md** from MainAvanues monorepo
4. **Deploy updated MCP server**
5. **Update IDEACODE documentation** with new commands

### Testing

**Test cases:**
- ✅ Valid folder (in registry)
- ✅ Valid folder (not in registry)
- ❌ Invalid folder (PascalCase)
- ❌ Invalid folder (type prefix)
- ⚠️ Folder variation (case difference)
- ⚠️ Folder variation (dash/underscore)
- ✅ Create folder with validation
- ✅ Update registry after creation

---

## 📝 Example Session

```bash
# User wants to create new authentication module
$ ideacode_validate_folder "common/feature-authentication"

❌ Error: Type prefix detected in folder name

Issue: Folder 'common/feature-authentication' has 'feature-' prefix
Rule: No type prefixes allowed in folder names
Suggestion: Use 'common/authentication' instead

Example:
  ❌ common/feature-authentication
  ✅ common/authentication

# User corrects the name
$ ideacode_create_folder "common/authentication" \
    --purpose "Authentication and authorization logic"

✅ Status: created
Message: Created 'common/authentication' and updated FOLDER-REGISTRY.md

Folder created at: /Volumes/M-Drive/Coding/AVA/common/authentication/
Registry updated: FOLDER-REGISTRY.md (added to Common section)

# Verify registration
$ ideacode_registry_search "authentication"

Results:
- common/authentication (Authentication and authorization logic)
Count: 1
```

---

## 📚 References

- **FOLDER-REGISTRY.md** - Master folder registry
- **PROJECT-INSTRUCTIONS.md** - Centralized project instructions
- **IDEACODE folder structure rules** - `/Volumes/M-Drive/Coding/ideacode/docs/ideacode/protocols/Protocol-File-Organization.md`
- **IDEACODE naming conventions** - `/Volumes/M-Drive/Coding/ideacode/updateideas/foldernaming.md`

---

**Version:** 1.0.0
**Status:** Specification Complete - Ready for Implementation
**Next Step:** Implement commands in ideacode-mcp server
