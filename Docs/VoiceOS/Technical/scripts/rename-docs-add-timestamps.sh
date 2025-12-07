#!/bin/bash

#==============================================================================
# VOS4 Documentation Naming Convention - Automated Rename Script
#==============================================================================
# Purpose: Rename all non-exempt documentation files to include timestamps
# Author: AI Documentation Agent
# Created: 2025-10-15 18:41:00 PDT
# Version: 1.0.0
#
# Usage:
#   ./rename-docs-add-timestamps.sh [--dry-run] [--backup]
#
# Options:
#   --dry-run   Show what would be renamed without actually renaming
#   --backup    Create backup before renaming (recommended)
#==============================================================================

set -e  # Exit on error

# Configuration
DOCS_DIR="/Volumes/M Drive/Coding/vos4/docs"
TIMESTAMP=$(date "+%y%m%d-%H%M")
LOG_FILE="/Volumes/M Drive/Coding/vos4/docs/Active/Rename-Log-${TIMESTAMP}.md"
BACKUP_DIR="/Volumes/M Drive/Coding/vos4/docs/archive/pre-rename-backup-${TIMESTAMP}"

# Flags
DRY_RUN=false
CREATE_BACKUP=false

# Parse arguments
for arg in "$@"; do
    case $arg in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --backup)
            CREATE_BACKUP=true
            shift
            ;;
        *)
            # Unknown option
            ;;
    esac
done

# Exemption patterns (based on approved policy)
declare -a EXEMPT_EXACT=(
    "README.md"
    "INDEX.md"
    "CLAUDE.md"
    "LICENSE.md"
    ".gitignore"
    "PROJECT-TODO-MASTER.md"
    "PROJECT-TODO-PRIORITY.md"
    "PROJECT-TODO-BACKLOG.md"
    "PROJECT-STATUS-CURRENT.md"
    "PROJECT-STATUS-SUMMARY.md"
    "PROJECT-STATUS-METRICS.md"
    "CHANGELOG-MASTER.md"
    "CHANGELOG-CURRENT.md"
    "changelog.md"
)

declare -a EXEMPT_PREFIXES=(
    "Protocol-"
    "Reference-"
    "Standards-"
    "Context-"
)

declare -a EXEMPT_SUFFIXES=(
    "-Template.md"
    "Template.md"
)

#==============================================================================
# Functions
#==============================================================================

# Initialize log file
init_log() {
    cat > "$LOG_FILE" << 'EOF'
<!--
filename: Rename-Log-TIMESTAMP.md
created: DATETIME PDT
author: AI Documentation Agent (Automated)
purpose: Log of automated documentation renaming operation
-->

# Documentation Renaming Log

**Operation Date:** DATETIME PDT
**Timestamp Used:** TIMESTAMP
**Mode:** MODE

---

## Summary

- **Total Files Scanned:** 0
- **Files Renamed:** 0
- **Files Skipped (Exempt):** 0
- **Files Skipped (Already Compliant):** 0
- **Errors:** 0

---

## Renamed Files

EOF

    # Replace placeholders
    sed -i '' "s/TIMESTAMP/${TIMESTAMP}/g" "$LOG_FILE"
    sed -i '' "s/DATETIME/$(date "+%Y-%m-%d %H:%M:%S")/g" "$LOG_FILE"
    if [ "$DRY_RUN" = true ]; then
        sed -i '' "s/MODE/DRY RUN (no changes made)/g" "$LOG_FILE"
    else
        sed -i '' "s/MODE/LIVE (files renamed)/g" "$LOG_FILE"
    fi
}

# Check if file is exempt
is_exempt() {
    local filename="$1"

    # Check exact matches
    for exempt in "${EXEMPT_EXACT[@]}"; do
        if [ "$filename" = "$exempt" ]; then
            return 0  # Is exempt
        fi
    done

    # Check prefix matches
    for prefix in "${EXEMPT_PREFIXES[@]}"; do
        if [[ "$filename" == "$prefix"* ]]; then
            return 0  # Is exempt
        fi
    done

    # Check suffix matches
    for suffix in "${EXEMPT_SUFFIXES[@]}"; do
        if [[ "$filename" == *"$suffix" ]]; then
            return 0  # Is exempt
        fi
    done

    return 1  # Not exempt
}

# Check if file already has timestamp
has_timestamp() {
    local filename="$1"

    # Check for pattern: YYMMDD-HHMM.md at end of filename
    if [[ "$filename" =~ [0-9]{6}-[0-9]{4}\.md$ ]]; then
        return 0  # Has timestamp
    fi

    return 1  # No timestamp
}

# Generate new filename with timestamp
generate_new_filename() {
    local original="$1"
    local base="${original%.md}"

    echo "${base}-${TIMESTAMP}.md"
}

# Create backup
create_backup() {
    if [ "$CREATE_BACKUP" = true ]; then
        echo "Creating backup in: $BACKUP_DIR"
        mkdir -p "$BACKUP_DIR"
        cp -R "$DOCS_DIR" "$BACKUP_DIR/"
        echo "✓ Backup created successfully"
    fi
}

# Log renamed file
log_rename() {
    local old_path="$1"
    local new_path="$2"
    local old_name=$(basename "$old_path")
    local new_name=$(basename "$new_path")
    local relative_dir="${old_path#$DOCS_DIR/}"
    relative_dir=$(dirname "$relative_dir")

    echo "| \`$old_name\` | \`$new_name\` | \`$relative_dir\` |" >> "$LOG_FILE"
}

#==============================================================================
# Main Processing
#==============================================================================

echo "=============================================================================="
echo " VOS4 Documentation Naming Convention - Automated Rename"
echo "=============================================================================="
echo ""
echo "Timestamp: $TIMESTAMP"
echo "Docs Directory: $DOCS_DIR"
echo "Log File: $LOG_FILE"
echo ""

if [ "$DRY_RUN" = true ]; then
    echo "MODE: DRY RUN (no changes will be made)"
else
    echo "MODE: LIVE (files will be renamed)"
fi

if [ "$CREATE_BACKUP" = true ]; then
    echo "Backup: ENABLED"
else
    echo "Backup: DISABLED"
fi

echo ""
echo "=============================================================================="
echo ""

# Confirm if not dry run
if [ "$DRY_RUN" = false ]; then
    read -p "This will rename files. Continue? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Operation cancelled."
        exit 1
    fi
fi

# Initialize
init_log
create_backup

# Counters
total_scanned=0
renamed=0
skipped_exempt=0
skipped_compliant=0
errors=0

# Add table header to log
echo "" >> "$LOG_FILE"
echo "| Old Filename | New Filename | Directory |" >> "$LOG_FILE"
echo "|--------------|--------------|-----------|" >> "$LOG_FILE"

# Process all .md files (excluding archives)
while IFS= read -r -d '' filepath; do
    ((total_scanned++))

    filename=$(basename "$filepath")
    dirpath=$(dirname "$filepath")

    # Skip if in archive folders
    if [[ "$filepath" == */archive/* ]] || [[ "$filepath" == */Archive/* ]]; then
        continue
    fi

    # Check if exempt
    if is_exempt "$filename"; then
        ((skipped_exempt++))
        if [ "$DRY_RUN" = true ]; then
            echo "[SKIP-EXEMPT] $filename"
        fi
        continue
    fi

    # Check if already has timestamp
    if has_timestamp "$filename"; then
        ((skipped_compliant++))
        if [ "$DRY_RUN" = true ]; then
            echo "[SKIP-COMPLIANT] $filename"
        fi
        continue
    fi

    # Generate new filename
    new_filename=$(generate_new_filename "$filename")
    new_filepath="$dirpath/$new_filename"

    # Check if new file already exists
    if [ -f "$new_filepath" ]; then
        echo "[ERROR] Target file already exists: $new_filename"
        ((errors++))
        continue
    fi

    # Rename file
    if [ "$DRY_RUN" = true ]; then
        echo "[RENAME] $filename -> $new_filename"
    else
        if mv "$filepath" "$new_filepath"; then
            echo "✓ Renamed: $filename -> $new_filename"
            log_rename "$filepath" "$new_filepath"
            ((renamed++))
        else
            echo "[ERROR] Failed to rename: $filename"
            ((errors++))
        fi
    fi

done < <(find "$DOCS_DIR" -type f -name "*.md" -print0)

#==============================================================================
# Finalize Log
#==============================================================================

# Update summary in log
sed -i '' "s/Total Files Scanned:\\*\\* 0/Total Files Scanned:** $total_scanned/g" "$LOG_FILE"
sed -i '' "s/Files Renamed:\\*\\* 0/Files Renamed:** $renamed/g" "$LOG_FILE"
sed -i '' "s/Files Skipped (Exempt):\\*\\* 0/Files Skipped (Exempt):** $skipped_exempt/g" "$LOG_FILE"
sed -i '' "s/Files Skipped (Already Compliant):\\*\\* 0/Files Skipped (Already Compliant):** $skipped_compliant/g" "$LOG_FILE"
sed -i '' "s/Errors:\\*\\* 0/Errors:** $errors/g" "$LOG_FILE"

# Add statistics footer
cat >> "$LOG_FILE" << EOF

---

## Statistics

- **Scanned:** $total_scanned files
- **Renamed:** $renamed files
- **Exempt:** $skipped_exempt files
- **Already Compliant:** $skipped_compliant files
- **Errors:** $errors

---

## Next Steps

1. Review renamed files in log above
2. Update cross-references in documentation
3. Verify all links still work
4. Run compliance audit to confirm 100%

**Log File:** \`$LOG_FILE\`
EOF

if [ "$CREATE_BACKUP" = true ]; then
    echo "**Backup Location:** \`$BACKUP_DIR\`" >> "$LOG_FILE"
fi

#==============================================================================
# Final Report
#==============================================================================

echo ""
echo "=============================================================================="
echo " Rename Operation Complete"
echo "=============================================================================="
echo ""
echo "Results:"
echo "  - Total Files Scanned: $total_scanned"
echo "  - Files Renamed: $renamed"
echo "  - Files Skipped (Exempt): $skipped_exempt"
echo "  - Files Skipped (Already Compliant): $skipped_compliant"
echo "  - Errors: $errors"
echo ""
echo "Log file created: $LOG_FILE"

if [ "$CREATE_BACKUP" = true ]; then
    echo "Backup created: $BACKUP_DIR"
fi

echo ""
echo "=============================================================================="

exit 0
