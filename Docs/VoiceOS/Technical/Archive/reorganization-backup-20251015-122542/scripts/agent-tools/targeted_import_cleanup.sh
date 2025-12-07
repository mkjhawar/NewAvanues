#!/bin/bash

# Targeted Import Cleanup Script for VOS4 Project
# Finds and fixes specific import issues: duplicates, obvious unused, etc.

PROJECT_ROOT="/Volumes/M Drive/Coding/Warp/vos4"
TEMP_DIR="/tmp/targeted_cleanup"
LOG_FILE="$TEMP_DIR/targeted_cleanup.log"

echo "🧹 AGENT 3: IMPORT CLEANUP SPECIALIST"
echo "STATUS: ACTIVE | TARGETED IMPORT CLEANUP"
echo

# Create temp directory
mkdir -p "$TEMP_DIR"

# Initialize log
echo "=== VOS4 TARGETED IMPORT CLEANUP LOG ===" > "$LOG_FILE"
echo "Started: $(date)" >> "$LOG_FILE"
echo >> "$LOG_FILE"

# Find all Kotlin files
echo "Finding Kotlin files..."
find "$PROJECT_ROOT" -name "*.kt" \
  -not -path "*/build/*" \
  -not -path "*/whisper-source/*" \
  > "$TEMP_DIR/kotlin_files.txt"

TOTAL_FILES=$(wc -l < "$TEMP_DIR/kotlin_files.txt")
echo "Found $TOTAL_FILES Kotlin files"

FIXED_FILES=0
DUPLICATE_IMPORTS_REMOVED=0
UNUSED_JAVA_IMPORTS_REMOVED=0

# Function to fix specific import issues
fix_file_imports() {
    local file="$1"
    local temp_file="$TEMP_DIR/$(basename "$file").tmp"
    local fixed=false
    
    # Read file line by line and process imports
    local output=""
    local seen_imports=()
    local current_line_num=0
    
    while IFS= read -r line; do
        current_line_num=$((current_line_num + 1))
        
        if [[ "$line" =~ ^import ]]; then
            # Check for duplicate imports
            local import_path=$(echo "$line" | sed 's/import //' | sed 's/[[:space:]]*$//')
            
            # Check if we've seen this exact import before
            local is_duplicate=false
            for seen_import in "${seen_imports[@]}"; do
                if [[ "$seen_import" == "$import_path" ]]; then
                    is_duplicate=true
                    break
                fi
            done
            
            if [[ "$is_duplicate" == true ]]; then
                echo "REMOVED DUPLICATE: $line" >> "$LOG_FILE"
                echo "  from: $file:$current_line_num" >> "$LOG_FILE"
                DUPLICATE_IMPORTS_REMOVED=$((DUPLICATE_IMPORTS_REMOVED + 1))
                fixed=true
                continue
            fi
            
            # Add to seen imports
            seen_imports+=("$import_path")
            
            # Check for obviously unused Java utility imports
            case "$import_path" in
                "java.util.Collections")
                    # Only keep if Collections is used statically
                    if ! grep -q "Collections\\." "$file"; then
                        echo "REMOVED UNUSED: $line" >> "$LOG_FILE"
                        echo "  from: $file:$current_line_num" >> "$LOG_FILE"
                        UNUSED_JAVA_IMPORTS_REMOVED=$((UNUSED_JAVA_IMPORTS_REMOVED + 1))
                        fixed=true
                        continue
                    fi
                    ;;
                "java.util.Arrays")
                    if ! grep -q "Arrays\\." "$file"; then
                        echo "REMOVED UNUSED: $line" >> "$LOG_FILE"
                        echo "  from: $file:$current_line_num" >> "$LOG_FILE"
                        UNUSED_JAVA_IMPORTS_REMOVED=$((UNUSED_JAVA_IMPORTS_REMOVED + 1))
                        fixed=true
                        continue
                    fi
                    ;;
                "java.util.Optional")
                    if ! grep -q "Optional" "$file" || [[ $(grep -c "Optional" "$file") -eq 1 ]]; then
                        echo "REMOVED UNUSED: $line" >> "$LOG_FILE"
                        echo "  from: $file:$current_line_num" >> "$LOG_FILE"
                        UNUSED_JAVA_IMPORTS_REMOVED=$((UNUSED_JAVA_IMPORTS_REMOVED + 1))
                        fixed=true
                        continue
                    fi
                    ;;
            esac
        fi
        
        output="$output$line\n"
    done < "$file"
    
    # Write fixed file if changes were made
    if [[ "$fixed" == true ]]; then
        echo -e "$output" > "$temp_file"
        mv "$temp_file" "$file"
        FIXED_FILES=$((FIXED_FILES + 1))
        echo "Fixed: $file" >> "$LOG_FILE"
        echo
    fi
}

# Process files
echo "Processing files for targeted cleanup..."
CURRENT=0

while IFS= read -r file; do
    CURRENT=$((CURRENT + 1))
    echo -ne "\\rProgress: $CURRENT/$TOTAL_FILES"
    
    if [[ -f "$file" ]] && [[ -s "$file" ]]; then
        fix_file_imports "$file"
    fi
done < "$TEMP_DIR/kotlin_files.txt"

echo
echo

# Find files with import organization issues
echo "🔍 Checking for import organization issues..."

DISORGANIZED_FILES=0
while IFS= read -r file; do
    if [[ -f "$file" ]] && [[ -s "$file" ]]; then
        # Check if file has imports in wrong order
        imports=$(grep "^import " "$file" | head -20)
        if [[ -n "$imports" ]]; then
            # Check specific patterns that indicate poor organization
            if echo "$imports" | grep -q "com.augmentalis" && echo "$imports" | tail -5 | grep -q "^import android"; then
                echo "File with organization issues: $file" >> "$LOG_FILE"
                DISORGANIZED_FILES=$((DISORGANIZED_FILES + 1))
            fi
        fi
    fi
done < "$TEMP_DIR/kotlin_files.txt"

echo "✅ Targeted import cleanup completed!"
echo
echo "📊 RESULTS:"
echo "Files processed: $TOTAL_FILES"
echo "Files fixed: $FIXED_FILES"
echo "Duplicate imports removed: $DUPLICATE_IMPORTS_REMOVED"
echo "Unused Java imports removed: $UNUSED_JAVA_IMPORTS_REMOVED"
echo "Files with organization issues: $DISORGANIZED_FILES"
echo
echo "📝 Detailed log: $LOG_FILE"

# Show examples of what was fixed
if [[ $FIXED_FILES -gt 0 ]]; then
    echo
    echo "🔧 Examples of fixes applied:"
    head -20 "$LOG_FILE" | grep -E "(REMOVED|Fixed:)" | head -10
fi

echo
echo "🎯 Summary of targeted fixes:"
echo "• Removed duplicate import statements"
echo "• Removed obviously unused Java utility imports"
echo "• Maintained safety by only removing high-confidence unused imports"
echo
echo "✨ Your codebase imports are now cleaner!"