#!/bin/bash
# IDEACODE Pre-Compact Hook
# Saves context before auto-compaction occurs

# Read JSON input from stdin
input=$(cat)

# Extract trigger type (auto or manual)
trigger_type=$(echo "$input" | jq -r '.trigger_type // "unknown"')
cwd=$(echo "$input" | jq -r '.cwd // "."')
transcript_path=$(echo "$input" | jq -r '.transcript_path // ""')

# Determine save directory
save_dir="$cwd/contextsave"
mkdir -p "$save_dir" 2>/dev/null

timestamp=$(date +%Y%m%d-%H%M%S)
save_file="$save_dir/pre-compact-$trigger_type-$timestamp.md"

# Save context before compaction
{
    echo "# Pre-Compact Context Save"
    echo ""
    echo "**Trigger:** $trigger_type"
    echo "**Timestamp:** $(date)"
    echo "**Working Directory:** $cwd"
    echo ""
    echo "---"
    echo ""

    # If transcript exists, include it
    if [[ -f "$transcript_path" ]]; then
        echo "## Transcript"
        echo ""
        cat "$transcript_path"
    else
        echo "## Note"
        echo ""
        echo "No transcript file available at compaction time."
        echo "This save captures the moment before context was compacted."
    fi
} > "$save_file" 2>/dev/null

# Keep only last 5 pre-compact saves (cleanup)
cd "$save_dir" 2>/dev/null && ls -t pre-compact-*.md 2>/dev/null | tail -n +6 | xargs rm -f 2>/dev/null

# Output confirmation (shown in hook output)
echo "Context saved: $save_file"
