#!/bin/bash
# WebAvanue Package Migration Script
# Migrates from dual namespace (Avanues.web + webavanue) to canonical com.augmentalis.webavanue
# Generated: 2025-12-13
# Estimated files: 207 Kotlin files

set -e  # Exit on error

echo "🔄 WebAvanue Package Migration Script"
echo "======================================"
echo ""
echo "This script will:"
echo "  1. Update package declarations in 207 Kotlin files"
echo "  2. Update import statements across all files"
echo "  3. Create commits after each major migration"
echo ""
echo "⚠️  WARNING: This is a BREAKING refactor"
echo "    - All imports will change"
echo "    - Build will break until complete"
echo "    - Fresh app install required after"
echo ""
read -p "Continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Migration cancelled"
    exit 1
fi

echo ""
echo "📊 Pre-migration analysis..."

# Count current packages
echo "Current package distribution:"
grep -r "^package " Modules/WebAvanue/universal/src --include="*.kt" | \
    awk '{print $2}' | sed 's/\.[^.]*$//' | sort | uniq -c | sort -rn | head -10

echo ""
echo "🔧 Starting migration..."
echo ""

# ============================================================================
# PHASE 1: Update presentation.ui.* → ui.screen.*
# ============================================================================
echo "📦 Phase 1: Migrating presentation.ui packages to ui.screen..."

find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/package com\.augmentalis\.Avanues\.web\.universal\.presentation\.ui\./package com.augmentalis.webavanue.ui.screen./g' {} \;

find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/import com\.augmentalis\.Avanues\.web\.universal\.presentation\.ui\./import com.augmentalis.webavanue.ui.screen./g' {} \;

git add -A
git commit -m "refactor(webavanue): migrate presentation.ui → ui.screen packages

- Updated package declarations
- Updated import statements
- Part 1/10 of package consolidation"

echo "✓ Phase 1 complete"
echo ""

# ============================================================================
# PHASE 2: Update presentation.controller → ui.viewmodel
# ============================================================================
echo "📦 Phase 2: Migrating presentation.controller to ui.viewmodel..."

find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/package com\.augmentalis\.Avanues\.web\.universal\.presentation\.controller/package com.augmentalis.webavanue.ui.viewmodel/g' {} \;

find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/import com\.augmentalis\.Avanues\.web\.universal\.presentation\.controller/import com.augmentalis.webavanue.ui.viewmodel/g' {} \;

git add -A
git commit -m "refactor(webavanue): migrate presentation.controller → ui.viewmodel

- Updated package declarations
- Updated import statements
- Part 2/10 of package consolidation"

echo "✓ Phase 2 complete"
echo ""

# ============================================================================
# PHASE 3: Update remaining presentation.* → ui.*
# ============================================================================
echo "📦 Phase 3: Migrating remaining presentation packages to ui..."

find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/package com\.augmentalis\.Avanues\.web\.universal\.presentation\./package com.augmentalis.webavanue.ui./g' {} \;

find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/import com\.augmentalis\.Avanues\.web\.universal\.presentation\./import com.augmentalis.webavanue.ui./g' {} \;

git add -A
git commit -m "refactor(webavanue): migrate remaining presentation.* → ui.*

- Updated package declarations
- Updated import statements
- Part 3/10 of package consolidation"

echo "✓ Phase 3 complete"
echo ""

# ============================================================================
# PHASE 4: Update download → feature.download
# ============================================================================
echo "📦 Phase 4: Migrating download to feature.download..."

find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/package com\.augmentalis\.Avanues\.web\.universal\.download/package com.augmentalis.webavanue.feature.download/g' {} \;

find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/import com\.augmentalis\.Avanues\.web\.universal\.download/import com.augmentalis.webavanue.feature.download/g' {} \;

git add -A
git commit -m "refactor(webavanue): migrate download → feature.download

- Part 4/10 of package consolidation"

echo "✓ Phase 4 complete"
echo ""

# ============================================================================
# PHASE 5: Update voice → feature.voice
# ============================================================================
echo "📦 Phase 5: Migrating voice to feature.voice..."

find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/package com\.augmentalis\.Avanues\.web\.universal\.voice/package com.augmentalis.webavanue.feature.voice/g' {} \;

find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/import com\.augmentalis\.Avanues\.web\.universal\.voice/import com.augmentalis.webavanue.feature.voice/g' {} \;

git add -A
git commit -m "refactor(webavanue): migrate voice → feature.voice

- Part 5/10 of package consolidation"

echo "✓ Phase 5 complete"
echo ""

# ============================================================================
# PHASE 6: Update xr → feature.xr
# ============================================================================
echo "📦 Phase 6: Migrating xr to feature.xr..."

find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/package com\.augmentalis\.Avanues\.web\.universal\.xr/package com.augmentalis.webavanue.feature.xr/g' {} \;

find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/import com\.augmentalis\.Avanues\.web\.universal\.xr/import com.augmentalis.webavanue.feature.xr/g' {} \;

git add -A
git commit -m "refactor(webavanue): migrate xr → feature.xr

- Part 6/10 of package consolidation"

echo "✓ Phase 6 complete"
echo ""

# ============================================================================
# PHASE 7: Update screenshot → feature.screenshot
# ============================================================================
echo "📦 Phase 7: Migrating screenshot to feature.screenshot..."

find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/package com\.augmentalis\.Avanues\.web\.universal\.screenshot/package com.augmentalis.webavanue.feature.screenshot/g' {} \;

find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/import com\.augmentalis\.Avanues\.web\.universal\.screenshot/import com.augmentalis.webavanue.feature.screenshot/g' {} \;

git add -A
git commit -m "refactor(webavanue): migrate screenshot → feature.screenshot

- Part 7/10 of package consolidation"

echo "✓ Phase 7 complete"
echo ""

# ============================================================================
# PHASE 8: Update commands → feature.commands
# ============================================================================
echo "📦 Phase 8: Migrating commands to feature.commands..."

find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/package com\.augmentalis\.Avanues\.web\.universal\.commands/package com.augmentalis.webavanue.feature.commands/g' {} \;

find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/import com\.augmentalis\.Avanues\.web\.universal\.commands/import com.augmentalis.webavanue.feature.commands/g' {} \;

git add -A
git commit -m "refactor(webavanue): migrate commands → feature.commands

- Part 8/10 of package consolidation"

echo "✓ Phase 8 complete"
echo ""

# ============================================================================
# PHASE 9: Consolidate util/utils → ui.util
# ============================================================================
echo "📦 Phase 9: Consolidating util and utils to ui.util..."

# Update util
find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/package com\.augmentalis\.Avanues\.web\.universal\.util\b/package com.augmentalis.webavanue.ui.util/g' {} \;

find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/import com\.augmentalis\.Avanues\.web\.universal\.util\./import com.augmentalis.webavanue.ui.util./g' {} \;

# Update utils (merge with util)
find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/package com\.augmentalis\.Avanues\.web\.universal\.utils/package com.augmentalis.webavanue.ui.util/g' {} \;

find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/import com\.augmentalis\.Avanues\.web\.universal\.utils/import com.augmentalis.webavanue.ui.util/g' {} \;

git add -A
git commit -m "refactor(webavanue): consolidate util + utils → ui.util

- Merged util and utils packages
- Part 9/10 of package consolidation"

echo "✓ Phase 9 complete"
echo ""

# ============================================================================
# PHASE 10: Update platform packages
# ============================================================================
echo "📦 Phase 10: Migrating platform packages..."

find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/package com\.augmentalis\.Avanues\.web\.universal\.platform/package com.augmentalis.webavanue.platform/g' {} \;

find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/import com\.augmentalis\.Avanues\.web\.universal\.platform/import com.augmentalis.webavanue.platform/g' {} \;

git add -A
git commit -m "refactor(webavanue): migrate platform packages

- universal.platform → webavanue.platform
- Part 10/10 of package consolidation"

echo "✓ Phase 10 complete"
echo ""

# ============================================================================
# FINAL PHASE: Catch any remaining Avanues.web references
# ============================================================================
echo "📦 Final Phase: Updating any remaining Avanues.web references..."

# Update any remaining Avanues.web.universal references
find Modules/WebAvanue/universal/src -name "*.kt" -type f -exec sed -i '' \
    's/com\.augmentalis\.Avanues\.web\.universal\./com.augmentalis.webavanue./g' {} \;

# Update app module references if any
find android/apps/webavanue/app/src -name "*.kt" -type f -exec sed -i '' \
    's/com\.augmentalis\.Avanues\.web\./com.augmentalis.webavanue./g' {} \; 2>/dev/null || true

git add -A
git commit -m "refactor(webavanue): final cleanup of Avanues.web references

- Replaced any remaining old namespace references
- Package consolidation complete (207 files migrated)" || echo "No remaining changes to commit"

echo ""
echo "✅ Migration Complete!"
echo ""
echo "📊 Post-migration analysis..."

# Count new packages
echo "New package distribution:"
grep -r "^package " Modules/WebAvanue/universal/src --include="*.kt" 2>/dev/null | \
    awk '{print $2}' | sed 's/\.[^.]*$//' | sort | uniq -c | sort -rn | head -10 || echo "No packages found"

echo ""
echo "📋 Summary:"
echo "  ✓ 10 migration phases completed"
echo "  ✓ ~207 Kotlin files updated"
echo "  ✓ Package depth reduced from 7-9 to 4-6 segments"
echo "  ✓ Dual namespace consolidated to com.augmentalis.webavanue"
echo ""
echo "⚠️  Next steps:"
echo "  1. Review git log for all migration commits"
echo "  2. Run: ./gradlew clean build (will likely have errors)"
echo "  3. Fix any remaining import issues in IDE"
echo "  4. Run tests: ./gradlew test"
echo "  5. Uninstall old app: adb uninstall com.augmentalis.Avanues.web"
echo "  6. Install new app: ./gradlew installDebug"
echo ""
echo "🎉 Package refactoring script completed!"
