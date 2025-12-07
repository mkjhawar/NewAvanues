#!/bin/bash

# Import Organization Script for VOS4 Project
# Organizes imports in the standard order

PROJECT_ROOT="/Volumes/M Drive/Coding/Warp/vos4"
TEMP_DIR="/tmp/import_organization"
LOG_FILE="$TEMP_DIR/organization.log"

echo "🧹 AGENT 3: IMPORT CLEANUP SPECIALIST"
echo "STATUS: ACTIVE | ORGANIZING IMPORTS"
echo

# Create temp directory
mkdir -p "$TEMP_DIR"

# Initialize log
echo "=== VOS4 IMPORT ORGANIZATION LOG ===" > "$LOG_FILE"
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

ORGANIZED_FILES=0

# Function to organize imports in a file
organize_file_imports() {
    local file="$1"
    local backup_file="$file.backup"
    local temp_file="$TEMP_DIR/$(basename "$file").tmp"
    
    # Skip if file is too small (likely no imports)
    if [[ $(wc -l < "$file") -lt 5 ]]; then
        return
    fi
    
    # Create backup
    cp "$file" "$backup_file"
    
    # Read file and separate sections
    local package_section=""
    local import_section=""
    local code_section=""
    local current_section="before_package"
    
    while IFS= read -r line; do
        case "$current_section" in
            "before_package")
                if [[ "$line" =~ ^package ]]; then
                    package_section="$line"
                    current_section="after_package"
                fi
                ;;
            "after_package")
                if [[ "$line" =~ ^import ]]; then
                    import_section="$import_section$line\n"
                    current_section="imports"
                elif [[ "$line" =~ ^[[:space:]]*$ ]]; then
                    # Skip empty lines after package
                    continue
                else
                    code_section="$code_section$line\n"
                    current_section="code"
                fi
                ;;
            "imports")
                if [[ "$line" =~ ^import ]]; then
                    import_section="$import_section$line\n"
                elif [[ "$line" =~ ^[[:space:]]*$ ]]; then
                    # Skip empty lines in import section
                    continue
                else
                    code_section="$code_section$line\n"
                    current_section="code"
                fi
                ;;
            "code")
                code_section="$code_section$line\n"
                ;;
        esac
    done < "$file"
    
    # If no imports found, skip
    if [[ -z "$import_section" ]]; then
        rm "$backup_file"
        return
    fi
    
    # Parse and categorize imports
    local android_imports=""
    local androidx_imports=""
    local kotlin_imports=""
    local java_imports=""
    local third_party_imports=""
    local project_imports=""
    
    while IFS= read -r import_line; do
        if [[ -z "$import_line" ]]; then
            continue
        fi
        
        case "$import_line" in
            *"android."*)
                android_imports="$android_imports$import_line\n"
                ;;
            *"androidx."*)
                androidx_imports="$androidx_imports$import_line\n"
                ;;
            *"kotlin"*|*"kotlinx."*)
                kotlin_imports="$kotlin_imports$import_line\n"
                ;;
            *"java."*|*"javax."*)
                java_imports="$java_imports$import_line\n"
                ;;
            *"com.augmentalis."*)
                project_imports="$project_imports$import_line\n"
                ;;
            *)
                third_party_imports="$third_party_imports$import_line\n"
                ;;
        esac
    done <<< "$(echo -e "$import_section")"
    
    # Sort each category alphabetically
    local sorted_imports=""
    
    if [[ -n "$java_imports" ]]; then
        sorted_imports="$sorted_imports$(echo -e "$java_imports" | sort -u)\n\n"
    fi
    
    if [[ -n "$android_imports" ]]; then
        sorted_imports="$sorted_imports$(echo -e "$android_imports" | sort -u)\n\n"
    fi
    
    if [[ -n "$androidx_imports" ]]; then
        sorted_imports="$sorted_imports$(echo -e "$androidx_imports" | sort -u)\n\n"
    fi
    
    if [[ -n "$kotlin_imports" ]]; then
        sorted_imports="$sorted_imports$(echo -e "$kotlin_imports" | sort -u)\n\n"
    fi
    
    if [[ -n "$third_party_imports" ]]; then
        sorted_imports="$sorted_imports$(echo -e "$third_party_imports" | sort -u)\n\n"
    fi
    
    if [[ -n "$project_imports" ]]; then
        sorted_imports="$sorted_imports$(echo -e "$project_imports" | sort -u)\n"
    fi
    
    # Remove trailing empty lines from imports
    sorted_imports=$(echo -e "$sorted_imports" | sed -e :a -e '/^\\s*$/N;s/\\n$//;ta')
    
    # Check if imports changed
    local original_sorted=$(echo -e "$import_section" | sort -u)
    local new_sorted=$(echo -e "$sorted_imports" | sort -u)
    
    if [[ "$original_sorted" != "$new_sorted" ]]; then
        # Write organized file
        {
            echo "$package_section"
            echo
            echo -e "$sorted_imports"
            echo
            echo -e "$code_section" | sed -e :a -e '/^\\s*$/N;$!ba;s/^\\n*//'
        } > "$temp_file"
        
        # Replace original with organized version
        mv "$temp_file" "$file"
        ORGANIZED_FILES=$((ORGANIZED_FILES + 1))
        
        echo "Organized: $file" >> "$LOG_FILE"
    else
        # No changes, remove backup
        rm "$backup_file"
    fi
}

# Process files
echo "Organizing imports..."
CURRENT=0

while IFS= read -r file; do
    CURRENT=$((CURRENT + 1))
    echo -ne "\\rProgress: $CURRENT/$TOTAL_FILES"
    
    if [[ -f "$file" ]] && [[ -s "$file" ]]; then
        organize_file_imports "$file"
    fi
done < "$TEMP_DIR/kotlin_files.txt"

echo
echo
echo "✅ Import organization completed!"
echo
echo "📊 RESULTS:"
echo "Files processed: $TOTAL_FILES"
echo "Files organized: $ORGANIZED_FILES"
echo
echo "📝 Detailed log: $LOG_FILE"
echo
echo "Standard import order applied:"
echo "1. Java/javax imports"
echo "2. Android imports"
echo "3. AndroidX imports"  
echo "4. Kotlin/kotlinx imports"
echo "5. Third-party imports"
echo "6. Project imports (com.augmentalis.*)"
echo
echo "⚠️  IMPORTANT: Backups created with .backup extension"
echo "   Test your build and remove backups when confident"