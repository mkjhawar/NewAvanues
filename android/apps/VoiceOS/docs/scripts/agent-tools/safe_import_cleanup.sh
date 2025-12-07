#!/bin/bash

# Safe Import Cleanup Script for VOS4 Project
# Only removes obviously unused imports with high confidence

PROJECT_ROOT="/Volumes/M Drive/Coding/vos4"
TEMP_DIR="/tmp/import_cleanup"
LOG_FILE="$TEMP_DIR/cleanup.log"

echo "🧹 AGENT 3: IMPORT CLEANUP SPECIALIST"
echo "STATUS: ACTIVE | PERFORMING SAFE IMPORT CLEANUP"
echo

# Create temp directory
mkdir -p "$TEMP_DIR"

# Initialize log
echo "=== VOS4 SAFE IMPORT CLEANUP LOG ===" > "$LOG_FILE"
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

CLEANED_FILES=0
REMOVED_IMPORTS=0

# Function to safely remove unused imports
clean_file() {
    local file="$1"
    local backup_file="$file.backup"
    local temp_file="$TEMP_DIR/$(basename "$file").tmp"
    
    # Create backup
    cp "$file" "$backup_file"
    
    # Read file content
    local content=$(cat "$file")
    local imports_section=""
    local code_section=""
    local in_imports=false
    local package_line=""
    
    # Extract imports and code separately
    while IFS= read -r line; do
        if [[ "$line" =~ ^package ]]; then
            package_line="$line"
        elif [[ "$line" =~ ^import ]]; then
            in_imports=true
            imports_section="$imports_section$line\n"
        elif [[ "$line" =~ ^[[:space:]]*$ ]] && [[ "$in_imports" == true ]]; then
            imports_section="$imports_section$line\n"
        else
            if [[ "$in_imports" == true ]]; then
                code_section="$code_section$line\n"
                in_imports=false
            else
                code_section="$code_section$line\n"
            fi
        fi
    done < "$file"
    
    # Analyze each import
    local cleaned_imports=""
    local file_cleaned=false
    
    while IFS= read -r import_line; do
        if [[ -z "$import_line" ]]; then
            cleaned_imports="$cleaned_imports\n"
            continue
        fi
        
        if [[ ! "$import_line" =~ ^import ]]; then
            cleaned_imports="$cleaned_imports$import_line\n"
            continue
        fi
        
        # Extract the imported item
        local import_path=$(echo "$import_line" | sed 's/import //' | sed 's/ as .*//')
        local class_name=""
        
        # Skip star imports - too risky to remove
        if [[ "$import_path" == *".*" ]]; then
            cleaned_imports="$cleaned_imports$import_line\n"
            continue
        fi
        
        # Get the simple class name
        if [[ "$import_line" == *" as "* ]]; then
            class_name=$(echo "$import_line" | sed 's/.* as //')
        else
            class_name=$(echo "$import_path" | sed 's/.*\.//')
        fi
        
        # Check if this import is used in the code section
        # Use multiple patterns to catch different usage scenarios
        local is_used=false
        
        # Pattern 1: Direct class usage
        if echo "$code_section" | grep -q "\\b$class_name\\b"; then
            is_used=true
        fi
        
        # Pattern 2: Constructor calls
        if echo "$code_section" | grep -q "$class_name("; then
            is_used=true
        fi
        
        # Pattern 3: Static access
        if echo "$code_section" | grep -q "$class_name\\."; then
            is_used=true
        fi
        
        # Pattern 4: Type annotations
        if echo "$code_section" | grep -q ": $class_name"; then
            is_used=true
        fi
        
        # Pattern 5: Generic types
        if echo "$code_section" | grep -q "<.*$class_name.*>"; then
            is_used=true
        fi
        
        # Additional checks for common false positives
        # Check for annotation usage
        if echo "$code_section" | grep -q "@$class_name"; then
            is_used=true
        fi
        
        # Check for companion object access
        if echo "$code_section" | grep -q "$class_name\\.Companion"; then
            is_used=true
        fi
        
        # Check for enum access
        if echo "$code_section" | grep -q "$class_name\\."; then
            is_used=true
        fi
        
        # Conservative approach: only remove if very confident it's unused
        # AND it's not a common framework import that might be used implicitly
        local safe_to_remove=false
        
        if [[ "$is_used" == false ]]; then
            # Additional safety checks - don't remove these common imports
            case "$import_path" in
                # Android framework
                "android."*) safe_to_remove=false ;;
                "androidx."*) safe_to_remove=false ;;
                # Kotlin standard
                "kotlin."*) safe_to_remove=false ;;
                "kotlinx."*) safe_to_remove=false ;;
                # Common annotations that might be used by reflection
                "*Test"*) safe_to_remove=false ;;
                "*Mock"*) safe_to_remove=false ;;
                "*Rule"*) safe_to_remove=false ;;
                # Project specific - only remove obvious ones
                "java.io.File") 
                    if ! echo "$code_section" | grep -q "File("; then
                        safe_to_remove=true
                    fi
                    ;;
                "java.util.Date")
                    if ! echo "$code_section" | grep -q "Date("; then
                        safe_to_remove=true
                    fi
                    ;;
                # Only remove very obvious unused imports
                "java.util.Collections")
                    if ! echo "$code_section" | grep -q "Collections\\."; then
                        safe_to_remove=true
                    fi
                    ;;
                *) safe_to_remove=false ;;
            esac
        fi
        
        if [[ "$safe_to_remove" == true ]]; then
            echo "REMOVED: $import_line" >> "$LOG_FILE"
            echo "  from: $file" >> "$LOG_FILE"
            file_cleaned=true
            REMOVED_IMPORTS=$((REMOVED_IMPORTS + 1))
        else
            cleaned_imports="$cleaned_imports$import_line\n"
        fi
        
    done <<< "$(echo -e "$imports_section")"
    
    # Write cleaned file if changes were made
    if [[ "$file_cleaned" == true ]]; then
        {
            [[ -n "$package_line" ]] && echo "$package_line" && echo
            echo -e "$cleaned_imports" | sed '/^$/N;/^\n$/d'  # Remove excessive empty lines
            echo
            echo -e "$code_section"
        } > "$temp_file"
        
        # Replace original with cleaned version
        mv "$temp_file" "$file"
        CLEANED_FILES=$((CLEANED_FILES + 1))
        
        echo "Cleaned: $file" >> "$LOG_FILE"
    else
        # No changes, remove backup
        rm "$backup_file"
    fi
}

# Process files
echo "Processing files for safe cleanup..."
CURRENT=0

while IFS= read -r file; do
    CURRENT=$((CURRENT + 1))
    echo -ne "\\rProgress: $CURRENT/$TOTAL_FILES"
    
    if [[ -f "$file" ]] && [[ -s "$file" ]]; then
        clean_file "$file"
    fi
done < "$TEMP_DIR/kotlin_files.txt"

echo
echo
echo "✅ Safe import cleanup completed!"
echo
echo "📊 RESULTS:"
echo "Files processed: $TOTAL_FILES"
echo "Files cleaned: $CLEANED_FILES"
echo "Imports removed: $REMOVED_IMPORTS"
echo
echo "📝 Detailed log: $LOG_FILE"
echo
echo "⚠️  IMPORTANT: Backups created with .backup extension"
echo "   Test your build and remove backups when confident"
echo
echo "💡 To remove all backups:"
echo "   find '$PROJECT_ROOT' -name '*.backup' -delete"