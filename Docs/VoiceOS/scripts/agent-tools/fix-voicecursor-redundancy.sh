#!/bin/bash

# Fix VoiceCursor path redundancy
# From: com.augmentalis.voiceos.voicecursor
# To: com.augmentalis.voiceos.cursor

echo "Fixing VoiceCursor path redundancy..."

# Base directories
OLD_DIR="/Volumes/M Drive/Coding/vos4/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/voicecursor"
NEW_DIR="/Volumes/M Drive/Coding/vos4/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor"

# Create new structure
echo "Creating new directory structure..."
mkdir -p "$NEW_DIR"

# Move all subdirectories
echo "Moving directories..."
for dir in "$OLD_DIR"/*; do
    if [ -d "$dir" ]; then
        dirname=$(basename "$dir")
        mv "$dir" "$NEW_DIR/$dirname"
        echo "Moved $dirname"
    fi
done

# Move any remaining files
echo "Moving remaining files..."
mv "$OLD_DIR"/*.kt "$NEW_DIR/" 2>/dev/null || true

# Remove old directory
rmdir "$OLD_DIR"

# Update package declarations in moved files
echo "Updating package declarations..."
find "$NEW_DIR" -name "*.kt" -type f -exec sed -i '' \
    's/package com.augmentalis.voiceos.voicecursor/package com.augmentalis.voiceos.cursor/g' {} \;

# Update imports across entire codebase
echo "Updating imports across codebase..."
find "/Volumes/M Drive/Coding/vos4" -name "*.kt" -type f -exec sed -i '' \
    's/import com.augmentalis.voiceos.voicecursor/import com.augmentalis.voiceos.cursor/g' {} \;

echo "Path redundancy fix complete!"
echo "Changed: com.augmentalis.voiceos.voicecursor -> com.augmentalis.voiceos.cursor"