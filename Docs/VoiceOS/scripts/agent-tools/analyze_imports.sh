#!/bin/bash

# Import Analysis Script for VOS4 Project
# Analyzes Kotlin files to identify potential unused imports

PROJECT_ROOT="/Volumes/M Drive/Coding/vos4"
TEMP_DIR="/tmp/import_analysis"
REPORT_FILE="$TEMP_DIR/import_analysis_report.txt"

echo "🧹 AGENT 3: IMPORT CLEANUP SPECIALIST"
echo "STATUS: ACTIVE | ANALYZING PROJECT IMPORTS"
echo

# Create temp directory
mkdir -p "$TEMP_DIR"

# Find all Kotlin files
echo "Finding Kotlin files..."
find "$PROJECT_ROOT" -name "*.kt" \
  -not -path "*/build/*" \
  -not -path "*/whisper-source/*" \
  > "$TEMP_DIR/kotlin_files.txt"

TOTAL_FILES=$(wc -l < "$TEMP_DIR/kotlin_files.txt")
echo "Found $TOTAL_FILES Kotlin files to analyze"

# Initialize report
echo "=== VOS4 PROJECT IMPORT ANALYSIS REPORT ===" > "$REPORT_FILE"
echo "Generated: $(date)" >> "$REPORT_FILE"
echo "Total files analyzed: $TOTAL_FILES" >> "$REPORT_FILE"
echo >> "$REPORT_FILE"

# Analyze each file
UNUSED_COUNT=0
FILES_WITH_UNUSED=0
CURRENT_FILE=0

echo
echo "Analyzing imports..."

while IFS= read -r file; do
    CURRENT_FILE=$((CURRENT_FILE + 1))
    echo -ne "\\rProgress: $CURRENT_FILE/$TOTAL_FILES"
    
    # Skip if file doesn't exist or is empty
    if [[ ! -f "$file" ]] || [[ ! -s "$file" ]]; then
        continue
    fi
    
    # Extract imports from file
    IMPORTS=$(grep "^import " "$file" | grep -v "^//" | sort -u)
    
    if [[ -z "$IMPORTS" ]]; then
        continue
    fi
    
    # Check each import for usage
    UNUSED_IMPORTS=""
    while IFS= read -r import_line; do
        if [[ -z "$import_line" ]]; then
            continue
        fi
        
        # Extract class name from import
        IMPORT_PATH=$(echo "$import_line" | sed 's/import //' | sed 's/ as .*//')
        CLASS_NAME=$(echo "$IMPORT_PATH" | sed 's/.*\\.//')
        
        # Skip star imports for now (they're complex to analyze)
        if [[ "$IMPORT_PATH" == *".*" ]]; then
            continue
        fi
        
        # Check if class is used in the file (excluding the import line itself)
        CONTENT_WITHOUT_IMPORTS=$(grep -v "^import " "$file")
        
        # Look for various usage patterns
        if ! echo "$CONTENT_WITHOUT_IMPORTS" | grep -q "\\b$CLASS_NAME\\b"; then
            UNUSED_IMPORTS="$UNUSED_IMPORTS\\n    $import_line"
            UNUSED_COUNT=$((UNUSED_COUNT + 1))
        fi
    done <<< "$IMPORTS"
    
    # Report unused imports for this file
    if [[ -n "$UNUSED_IMPORTS" ]]; then
        FILES_WITH_UNUSED=$((FILES_WITH_UNUSED + 1))
        echo >> "$REPORT_FILE"
        echo "FILE: $(echo "$file" | sed "s|$PROJECT_ROOT/||")" >> "$REPORT_FILE"
        echo "Potentially unused imports:" >> "$REPORT_FILE"
        echo -e "$UNUSED_IMPORTS" >> "$REPORT_FILE"
    fi
    
done < "$TEMP_DIR/kotlin_files.txt"

echo
echo
echo "Analysis complete!"
echo

# Summary
echo "=== SUMMARY ===" >> "$REPORT_FILE"
echo "Files with potentially unused imports: $FILES_WITH_UNUSED" >> "$REPORT_FILE"
echo "Total potentially unused imports: $UNUSED_COUNT" >> "$REPORT_FILE"
echo >> "$REPORT_FILE"
echo "NOTE: This analysis may have false positives. Please review before removing imports." >> "$REPORT_FILE"

# Display summary
echo "📊 ANALYSIS SUMMARY:"
echo "Files with potentially unused imports: $FILES_WITH_UNUSED"
echo "Total potentially unused imports: $UNUSED_COUNT"
echo
echo "Full report saved to: $REPORT_FILE"

# Show top 10 files with most unused imports
echo
echo "🔍 TOP FILES WITH UNUSED IMPORTS:"
echo "=================================="

if [[ $FILES_WITH_UNUSED -gt 0 ]]; then
    # Extract file sections and count unused imports per file
    awk '
        /^FILE:/ { 
            current_file = $0
            unused_count = 0
        }
        /^    import / { unused_count++ }
        /^$/ && unused_count > 0 { 
            print unused_count " " current_file
            unused_count = 0
        }
    ' "$REPORT_FILE" | sort -nr | head -10
else
    echo "No files with unused imports found!"
fi

echo
echo "📝 To review the complete analysis, run:"
echo "   cat '$REPORT_FILE'"