#!/bin/bash
# VOS to AVU Batch Converter
# Converts all .vos JSON files to .avu format
# Usage: ./scripts/convert-vos-to-avu.sh

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "============================================================"
echo "VOS to AVU Batch Converter"
echo "============================================================"
echo ""

# Stats
TOTAL=0
CONVERTED=0
SKIPPED=0
ERRORS=0
TOTAL_JSON_SIZE=0
TOTAL_AVU_SIZE=0

# Directories to process
DIRS=(
    "Modules/AvaMagic/managers/CommandManager/src/main/assets/commands"
    "Modules/VoiceOS/managers/CommandManager/src/main/assets/commands"
)

# Locales
LOCALES=("en-US" "es-ES" "de-DE" "fr-FR")

convert_file() {
    local vos_file="$1"
    local avu_file="${vos_file%.vos}.avu"
    local filename=$(basename "$vos_file")

    # Skip if AVU already exists
    if [ -f "$avu_file" ]; then
        echo -e "  ${YELLOW}[SKIP]${NC} $filename (AVU exists)"
        SKIPPED=$((SKIPPED + 1))
        return 0
    fi

    # Read JSON and extract fields using python
    local result
    result=$(python3 << PYEOF
import json
import sys

try:
    with open('$vos_file', 'r') as f:
        data = json.load(f)

    schema = data.get('schema', 'vos-1.0')
    version = data.get('version', '1.0.0')
    locale = data.get('locale', 'en-US')

    file_info = data.get('file_info', {})
    category = file_info.get('category', 'unknown')
    display_name = file_info.get('display_name', category)
    description = file_info.get('description', '')
    command_count = file_info.get('command_count', 0)
    orig_filename = file_info.get('filename', '$filename')

    commands = data.get('commands', [])

    # Build AVU content
    lines = []
    lines.append('# AVU Format v1.0')
    lines.append('# Type: VOICE')
    lines.append('# Extension: .avu')
    lines.append(f'# Converted from: {orig_filename}')
    lines.append('---')
    lines.append('schema: avu-vos-1.0')
    lines.append(f'version: {version}')
    lines.append(f'locale: {locale}')
    lines.append('project: voiceos')
    lines.append('metadata:')
    lines.append(f'  category: {category}')
    lines.append(f'  display_name: {display_name}')
    lines.append(f'  description: {description}')
    lines.append(f'  command_count: {command_count}')
    lines.append('---')

    # Category
    lines.append(f'CAT:{category}:{display_name}:{description}')

    # Commands
    synonyms = {}
    for cmd in commands:
        action = cmd.get('action', '')
        primary = cmd.get('cmd', '')
        syns = cmd.get('syn', [])

        if action and primary:
            lines.append(f'CMD:{action}:{primary}')
            if syns:
                synonyms[action] = syns

    # Synonyms section
    if synonyms:
        lines.append('---')
        for action, syns in synonyms.items():
            syn_str = ','.join(syns)
            lines.append(f'SYN:{action}:[{syn_str}]')

    print('\\n'.join(lines))

except Exception as e:
    print(f'ERROR: {e}', file=sys.stderr)
    sys.exit(1)
PYEOF
)

    if [ $? -ne 0 ]; then
        echo -e "  ${RED}[ERR]${NC} $filename: Conversion failed"
        ERRORS=$((ERRORS + 1))
        return 1
    fi

    # Write AVU file
    echo "$result" > "$avu_file"

    # Calculate sizes
    local json_size=$(wc -c < "$vos_file" | tr -d ' ')
    local avu_size=$(wc -c < "$avu_file" | tr -d ' ')

    if [ "$json_size" -gt 0 ]; then
        local reduction=$(( (json_size - avu_size) * 100 / json_size ))
        TOTAL_JSON_SIZE=$((TOTAL_JSON_SIZE + json_size))
        TOTAL_AVU_SIZE=$((TOTAL_AVU_SIZE + avu_size))
        echo -e "  ${GREEN}[OK]${NC} $filename -> ${filename%.vos}.avu (${reduction}% smaller)"
    else
        echo -e "  ${GREEN}[OK]${NC} $filename -> ${filename%.vos}.avu"
    fi

    CONVERTED=$((CONVERTED + 1))
    return 0
}

# Process each directory
for base_dir in "${DIRS[@]}"; do
    if [ ! -d "$base_dir" ]; then
        echo "Directory not found: $base_dir"
        continue
    fi

    echo "Processing: $base_dir"

    for locale in "${LOCALES[@]}"; do
        locale_dir="$base_dir/$locale"

        if [ ! -d "$locale_dir" ]; then
            continue
        fi

        # Find all .vos files
        for vos_file in "$locale_dir"/*.vos; do
            if [ -f "$vos_file" ]; then
                TOTAL=$((TOTAL + 1))
                convert_file "$vos_file" || true
            fi
        done
    done

    echo ""
done

# Summary
echo "============================================================"
echo "Conversion Summary"
echo "============================================================"
echo "Total files:    $TOTAL"
echo "Converted:      $CONVERTED"
echo "Skipped:        $SKIPPED"
echo "Errors:         $ERRORS"

if [ "$TOTAL_JSON_SIZE" -gt 0 ]; then
    OVERALL_REDUCTION=$(( (TOTAL_JSON_SIZE - TOTAL_AVU_SIZE) * 100 / TOTAL_JSON_SIZE ))
    echo ""
    echo "Total JSON size: $((TOTAL_JSON_SIZE / 1024)) KB"
    echo "Total AVU size:  $((TOTAL_AVU_SIZE / 1024)) KB"
    echo "Overall reduction: ${OVERALL_REDUCTION}%"
fi

echo ""
echo "Done!"
