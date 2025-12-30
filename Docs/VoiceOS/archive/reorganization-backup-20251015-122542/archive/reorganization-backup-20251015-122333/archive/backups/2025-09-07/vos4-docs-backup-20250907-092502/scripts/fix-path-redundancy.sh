#!/bin/bash

# Fix Path Redundancy in VoiceAccessibility
# Moves from voiceaccessibility to voiceos.accessibility

echo "=== Fixing Path Redundancy in VoiceAccessibility ==="

OLD_BASE="/Volumes/M Drive/Coding/Warp/vos4/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceaccessibility"
NEW_BASE="/Volumes/M Drive/Coding/Warp/vos4/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility"
TEST_OLD_BASE="/Volumes/M Drive/Coding/Warp/vos4/apps/VoiceAccessibility/src/test/java/com/augmentalis/voiceaccessibility"
TEST_NEW_BASE="/Volumes/M Drive/Coding/Warp/vos4/apps/VoiceAccessibility/src/test/java/com/augmentalis/voiceos/accessibility"

# Create new directory structure if needed
mkdir -p "$NEW_BASE"
mkdir -p "$TEST_NEW_BASE"

# Move directories from old to new location
echo "Moving directories..."
for dir in config extractors handlers managers recognition service viewmodel; do
    if [ -d "$OLD_BASE/$dir" ]; then
        echo "  Moving $dir..."
        mv "$OLD_BASE/$dir" "$NEW_BASE/"
    fi
done

# The ui directory might have conflicts, so merge it
if [ -d "$OLD_BASE/ui" ]; then
    echo "  Merging ui directory..."
    cp -r "$OLD_BASE/ui/"* "$NEW_BASE/ui/" 2>/dev/null || true
    rm -rf "$OLD_BASE/ui"
fi

# Move test files
if [ -d "$TEST_OLD_BASE" ]; then
    echo "Moving test files..."
    mv "$TEST_OLD_BASE/"* "$TEST_NEW_BASE/" 2>/dev/null || true
fi

# Remove old directories if empty
rmdir "$OLD_BASE" 2>/dev/null || true
rmdir "$TEST_OLD_BASE" 2>/dev/null || true

echo "=== Updating Package Declarations ==="

# Update all Kotlin files to use new package
find "$NEW_BASE" -name "*.kt" -type f | while read -r file; do
    # Update package declaration
    sed -i '' 's/package com\.augmentalis\.voiceaccessibility/package com.augmentalis.voiceos.accessibility/g' "$file"
    
    # Update imports
    sed -i '' 's/import com\.augmentalis\.voiceaccessibility/import com.augmentalis.voiceos.accessibility/g' "$file"
done

# Update test files
find "$TEST_NEW_BASE" -name "*.kt" -type f 2>/dev/null | while read -r file; do
    sed -i '' 's/package com\.augmentalis\.voiceaccessibility/package com.augmentalis.voiceos.accessibility/g' "$file"
    sed -i '' 's/import com\.augmentalis\.voiceaccessibility/import com.augmentalis.voiceos.accessibility/g' "$file"
done

# Update imports in other modules that might reference this
echo "=== Updating References in Other Modules ==="
find "/Volumes/M Drive/Coding/Warp/vos4" -name "*.kt" -type f -not -path "*/voiceaccessibility/*" | while read -r file; do
    if grep -q "com.augmentalis.voiceaccessibility" "$file"; then
        echo "  Updating: $file"
        sed -i '' 's/import com\.augmentalis\.voiceaccessibility/import com.augmentalis.voiceos.accessibility/g' "$file"
    fi
done

echo "=== Path Redundancy Fix Complete ==="
echo ""
echo "Summary:"
echo "  Old path: com.augmentalis.voiceaccessibility"
echo "  New path: com.augmentalis.voiceos.accessibility"
echo ""
echo "Next steps:"
echo "  1. Review the changes"
echo "  2. Update any XML files if needed"
echo "  3. Clean and rebuild the project"