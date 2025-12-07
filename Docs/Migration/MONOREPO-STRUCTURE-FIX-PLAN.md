# NewAvanues Monorepo Structure Fix Plan

**Created:** 2025-12-07
**Status:** Ready for Execution
**Priority:** HIGH - Blocks future migrations

---

## Problem Statement

Current structure **violates** documented guidelines in FOLDER-REGISTRY.md and NewAvanues CLAUDE.md.

### Current Structure (WRONG):

```
Common/
├── Database/              ✅ Cross-product shared
├── NLU/                   ✅ Cross-product shared
├── UI/                    ✅ Cross-product shared
├── Utils/                 ✅ Cross-product shared
├── Libraries/             ❌ REDUNDANT (violates NO REDUNDANT NAMES)
│   └── VoiceOS/           ❌ Product-specific (should be in Modules/)
│       └── core/          ❌ Redundant nesting
│           ├── accessibility-types
│           ├── command-models
│           ├── constants
│           ├── database
│           ├── exceptions
│           ├── hash
│           ├── json-utils
│           ├── result
│           ├── text-utils
│           ├── validation
│           └── voiceos-logging
└── VoiceOS/               ❌ Empty, leftover from previous migration
```

### Target Structure (CORRECT):

```
Common/
├── Database/              ✅ Cross-product shared KMP
├── NLU/                   ✅ Cross-product shared KMP
├── UI/                    ✅ Cross-product shared KMP
├── Utils/                 ✅ Cross-product shared KMP
└── ThirdParty/            ✅ Cross-product dependencies
    ├── Vivoka/
    └── Vosk/

Modules/
└── VoiceOS/               ✅ Product-specific KMP modules
    ├── core/              ✅ Low-level VoiceOS utilities (moved from Common/)
    │   ├── accessibility-types
    │   ├── command-models
    │   ├── constants
    │   ├── database
    │   ├── exceptions
    │   ├── hash
    │   ├── json-utils
    │   ├── result
    │   ├── text-utils
    │   ├── validation
    │   └── voiceos-logging
    ├── libraries/         ✅ VoiceOS feature libraries
    ├── managers/          ✅ VoiceOS managers
    └── apps/              ✅ VoiceOS applications
```

---

## Design Principle

**Common/ vs Modules/ Distinction:**

| Folder | Purpose | Scope | Examples |
|--------|---------|-------|----------|
| **Common/** | Cross-product shared KMP code | Used by **multiple products** | Database, NLU, UI, Utils, ThirdParty |
| **Modules/** | Product-specific KMP code | Used by **single product only** | Modules/VoiceOS/, Modules/WebAvanue/, Modules/AvaConnect/ |

**Decision Tree:**
```
Is this code shared across VoiceOS, WebAvanue, AVA, AvaConnect?
├─ YES → Common/{Domain}/
└─ NO → Modules/{Product}/
```

---

## Migration Steps

### Phase 1: Move VoiceOS Core Modules

**Action:** Move `Common/Libraries/VoiceOS/core/*` → `Modules/VoiceOS/core/`

**Modules to Move (13 total):**
1. accessibility-types
2. command-models
3. constants
4. database
5. exceptions
6. hash
7. json-utils
8. result
9. text-utils
10. validation
11. voiceos-logging
12. QUICK-START.md
13. README.md

**Commands:**
```bash
# Create target directory
mkdir -p Modules/VoiceOS/core

# Move all modules
mv Common/Libraries/VoiceOS/core/* Modules/VoiceOS/core/

# Remove empty redundant folders
rm -rf Common/Libraries/VoiceOS
rm -rf Common/Libraries  # Should be empty now
rm -rf Common/VoiceOS    # Empty leftover
```

### Phase 2: Update Gradle References

**Files to Update:**

1. **VoiceOS app settings.gradle.kts** (`android/apps/VoiceOS/settings.gradle.kts`)
   - Change: `:Common:Libraries:VoiceOS:core:` → `:Modules:VoiceOS:core:`
   - Change: `../../../Common/Libraries/VoiceOS/core/` → `../../../Modules/VoiceOS/core/`
   - **Estimated:** ~22 module references

2. **VoiceOS app build.gradle.kts** (`android/apps/VoiceOS/app/build.gradle.kts`)
   - Change: `project(":Common:Libraries:VoiceOS:core:*")` → `project(":Modules:VoiceOS:core:*")`

3. **Modules/VoiceOS/ build files**
   - Update any cross-references from libraries/ and managers/ to core/
   - Pattern: `implementation(project(":Common:Libraries:VoiceOS:core:*"))`
   - Replace: `implementation(project(":Modules:VoiceOS:core:*"))`

**Batch Update Command:**
```bash
# Find all build files that reference old path
find android Modules -name "*.gradle.kts" -exec grep -l "Common:Libraries:VoiceOS:core" {} \;

# Replace references (requires verification first)
find android Modules -name "*.gradle.kts" -exec sed -i.bak 's|:Common:Libraries:VoiceOS:core:|:Modules:VoiceOS:core:|g' {} \;
find android Modules -name "*.gradle.kts" -exec sed -i.bak 's|Common/Libraries/VoiceOS/core/|Modules/VoiceOS/core/|g' {} \;
```

### Phase 3: Verification

**Tests:**
```bash
# 1. Verify no old references remain
grep -r "Common/Libraries/VoiceOS" android Modules

# 2. Verify Gradle sync
cd android/apps/VoiceOS
./gradlew projects

# 3. Expected output includes:
# +--- Project ':Modules:VoiceOS:core:result'
# +--- Project ':Modules:VoiceOS:core:hash'
# etc.

# 4. Clean build test
./gradlew clean build
```

### Phase 4: Cleanup

```bash
# Remove .bak files from sed operations
find android Modules -name "*.bak" -delete

# Verify structure is clean
ls -la Common/       # Should NOT have Libraries/ or VoiceOS/
ls -la Modules/VoiceOS/core/  # Should have 11 modules + 2 docs
```

---

## Enforcement Rules for Future Migrations

### Pre-Migration Checklist

Before importing ANY new project (WebAvanue, AvaConnect, AVA, Avanues):

**1. Analyze Code Scope**
```
For each module in source project:
  Question: "Is this used by multiple products?"
  ├─ YES → Destination: Common/{Domain}/
  │   Examples: Database utilities, UI components, API clients
  └─ NO → Destination: Modules/{Product}/
      Examples: Product-specific features, business logic
```

**2. Check Common/ Naming**
```
If placing in Common/:
  - Domain name must be GENERIC (Database, UI, Utils)
  - NOT product-specific (NO VoiceOS, WebAvanue, AVA)
  - PascalCase required
  - No redundant "Libraries" or "Common" in path
```

**3. Check Modules/ Naming**
```
If placing in Modules/:
  - Top level = Product name (VoiceOS, WebAvanue, AvaConnect)
  - Second level = Module type (core, features, apps, etc.)
  - PascalCase required
  - NO redundant "Modules" or "libraries" in child paths
```

### Migration Template

```bash
# Step 1: ANALYZE
echo "Analyzing {Project} for Common vs Modules split..."
ls -R /path/to/{Project}/src

# Step 2: CATEGORIZE
# List modules that are cross-product (→ Common/)
# List modules that are product-specific (→ Modules/{Product}/)

# Step 3: VALIDATE NAMING
# Check for redundant folder names
# Apply NO REDUNDANT NAMES rule

# Step 4: MIGRATE
# Move cross-product → Common/{Domain}/
# Move product-specific → Modules/{Product}/

# Step 5: UPDATE GRADLE
# Update settings.gradle.kts
# Update build.gradle.kts dependencies
# Fix module paths

# Step 6: VERIFY
# ./gradlew projects
# ./gradlew clean build
# grep for old paths
```

### Automated Validation Script

Create `.ideacode/scripts/validate-monorepo-structure.sh`:

```bash
#!/bin/bash

echo "=== Validating Monorepo Structure ==="

# Rule 1: Common/ should only contain cross-product domains
echo "Checking Common/ for product-specific folders..."
if ls Common/ | grep -E "VoiceOS|WebAvanue|AvaConnect|AVA|Avanues"; then
  echo "❌ FAIL: Product-specific folders in Common/"
  exit 1
fi

# Rule 2: Common/ should not have "Libraries" subfolder
if [ -d "Common/Libraries" ]; then
  echo "❌ FAIL: Redundant Common/Libraries/ folder exists"
  exit 1
fi

# Rule 3: Modules/ should be organized by product
echo "Checking Modules/ structure..."
for product in Modules/*/; do
  product_name=$(basename "$product")
  if [ -d "$product/libraries" ] && [ -d "$product/managers" ]; then
    echo "✅ PASS: $product_name has proper substructure"
  fi
done

# Rule 4: No "Common" references in Gradle for product modules
echo "Checking for incorrect Gradle references..."
if grep -r ":Common:.*:VoiceOS:" android Modules --include="*.gradle.kts"; then
  echo "❌ FAIL: Found :Common:*:VoiceOS: references (should be :Modules:VoiceOS:)"
  exit 1
fi

echo "✅ ALL CHECKS PASSED"
```

---

## Updated FOLDER-REGISTRY.md Rules

Add this section to `/Volumes/M-Drive/Coding/ideacode/.ideacode/registries/FOLDER-REGISTRY.md`:

```markdown
## Common/ vs Modules/ Placement Rule

**Common/** - Cross-product shared KMP libraries
- Used by MULTIPLE products (VoiceOS, WebAvanue, AVA, AvaConnect)
- Generic domain names: Database, UI, NLU, Utils, ThirdParty
- NO product names in paths

**Modules/** - Product-specific KMP modules
- Used by SINGLE product only
- Organized by product: Modules/{Product}/
- May have internal structure: core/, features/, apps/, etc.

**Decision Process:**
1. Ask: "Will VoiceOS, WebAvanue, AND AVA all use this?"
   - YES → Common/{Domain}/
   - NO → Modules/{Product}/

2. Check naming:
   - Common/VoiceOS/ ❌ WRONG (product-specific)
   - Modules/VoiceOS/ ✅ CORRECT

3. Verify no redundancy:
   - Common/Libraries/Database/ ❌ WRONG (redundant "Libraries")
   - Common/Database/ ✅ CORRECT
```

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Break Gradle build | HIGH | Create git tag before migration, verify with `./gradlew projects` |
| Miss some references | MEDIUM | Use grep to find all old paths before/after |
| Wrong Gradle path syntax | MEDIUM | Test with single module first, then batch |
| Delete wrong folders | HIGH | Use `mv` not `rm`, verify with `ls` before cleanup |

---

## Rollback Plan

If migration fails:

```bash
# Revert to pre-migration state
git reset --hard monorepo-structure-pre-fix

# Or restore from tag
git tag monorepo-structure-pre-fix
git reset --hard monorepo-structure-pre-fix
```

---

## Success Criteria

✅ No folders in `Common/` with product names (VoiceOS, WebAvanue, etc.)
✅ No `Common/Libraries/` redundant folder
✅ All VoiceOS core modules in `Modules/VoiceOS/core/`
✅ `./gradlew projects` shows `:Modules:VoiceOS:core:*` paths
✅ `./gradlew clean build` succeeds
✅ No grep results for old paths in Gradle files
✅ Validation script passes

---

## Future Migrations Reference

When migrating WebAvanue, AvaConnect, AVA, or Avanues:

1. **READ THIS DOCUMENT FIRST**
2. Apply Common/ vs Modules/ decision tree
3. Check for product-specific code in Common/ ← FORBIDDEN
4. Use validation script before committing
5. Follow migration template

---

**Prepared by:** Claude (IDEACODE v10.3)
**Approved by:** [Pending User Review]
**Execution Status:** Ready
