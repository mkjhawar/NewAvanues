#!/bin/bash

# RAG Package Migration Script
# Migrates from com.augmentalis.ava.features.rag to com.augmentalis.rag
# Flattens data.handlers -> handlers and data.clustering -> clustering

RAG_DIR="/Volumes/M-Drive/Coding/NewAvanues-AVA/Modules/AVA/RAG"
cd "$RAG_DIR"

echo "Starting RAG package migration..."
echo "Working directory: $(pwd)"

# Function to get new package path from old path
get_new_path() {
    local old_path="$1"
    local new_path="$old_path"

    # Replace base package
    new_path="${new_path//com\/augmentalis\/ava\/features\/rag/com\/augmentalis\/rag}"

    # Flatten data/handlers -> handlers
    new_path="${new_path//\/data\/handlers/\/handlers}"

    # Flatten data/clustering -> clustering
    new_path="${new_path//\/data\/clustering/\/clustering}"

    echo "$new_path"
}

# Function to update file contents
update_file_contents() {
    local file="$1"

    echo "Processing: $file"

    # Update package declarations
    sed -i '' 's/^package com\.augmentalis\.ava\.features\.rag\.data\.handlers/package com.augmentalis.rag.handlers/g' "$file"
    sed -i '' 's/^package com\.augmentalis\.ava\.features\.rag\.data\.clustering/package com.augmentalis.rag.clustering/g' "$file"
    sed -i '' 's/^package com\.augmentalis\.ava\.features\.rag/package com.augmentalis.rag/g' "$file"

    # Update imports
    sed -i '' 's/import com\.augmentalis\.ava\.features\.rag\.data\.handlers/import com.augmentalis.rag.handlers/g' "$file"
    sed -i '' 's/import com\.augmentalis\.ava\.features\.rag\.data\.clustering/import com.augmentalis.rag.clustering/g' "$file"
    sed -i '' 's/import com\.augmentalis\.ava\.features\.rag/import com.augmentalis.rag/g' "$file"
}

# Create new directory structure
echo "Creating new directory structure..."

for source_set in androidMain commonMain iosMain desktopMain androidTest commonTest; do
    if [ -d "src/$source_set/kotlin" ]; then
        mkdir -p "src/$source_set/kotlin/com/augmentalis/rag"
    fi
done

# Process all .kt files
echo "Processing Kotlin files..."
file_count=0

find src -name "*.kt" -type f | while read -r file; do
    # Get relative path from src/
    rel_path="${file#src/}"

    # Get source set (androidMain, commonMain, etc)
    source_set=$(echo "$rel_path" | cut -d'/' -f1)

    # Get file path after kotlin/
    after_kotlin=$(echo "$rel_path" | grep -o 'kotlin/.*')
    file_path="${after_kotlin#kotlin/}"

    # Calculate new path
    new_file_path=$(get_new_path "$file_path")
    new_full_path="src/$source_set/kotlin/$new_file_path"

    # Create directory for new file
    new_dir=$(dirname "$new_full_path")
    mkdir -p "$new_dir"

    # Update file contents
    update_file_contents "$file"

    # Move file to new location
    if [ "$file" != "$new_full_path" ]; then
        mv "$file" "$new_full_path"
        echo "  Moved: $file -> $new_full_path"
    else
        echo "  Updated: $file"
    fi

    ((file_count++))
done

echo ""
echo "Files processed: $file_count"

# Update build.gradle.kts
echo ""
echo "Updating build.gradle.kts..."
sed -i '' 's/namespace = "com\.augmentalis\.ava\.features\.rag"/namespace = "com.augmentalis.rag"/g' build.gradle.kts

# Clean up old empty directories
echo ""
echo "Cleaning up old empty directories..."
find src -type d -empty -delete

echo ""
echo "Migration complete!"
echo ""
echo "Summary:"
echo "- Files migrated: $file_count"
echo "- Old package: com.augmentalis.ava.features.rag"
echo "- New package: com.augmentalis.rag"
echo "- Flattened: data.handlers -> handlers"
echo "- Flattened: data.clustering -> clustering"
echo "- Updated: build.gradle.kts namespace"
