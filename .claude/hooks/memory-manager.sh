#!/bin/bash
# IDEACODE Memory Manager v1.2
# Task-aware context management with auto-save and recommendations
# PER-REPO + PER-SESSION isolation - multiple terminals in same repo supported
#
# Usage:
#   memory-manager.sh check     - Get current status and recommendation
#   memory-manager.sh save      - Save current state to memory
#   memory-manager.sh load      - Load memory state
#   memory-manager.sh task-start "description" - Mark task as active
#   memory-manager.sh task-end   - Mark task as complete
#   memory-manager.sh decision "CATEGORY: decision" - Record a decision (shared)

# Determine repo root and create repo-specific paths
REPO_ROOT="${CWD:-.}"
if git -C "$REPO_ROOT" rev-parse --git-dir > /dev/null 2>&1; then
    REPO_ROOT=$(git -C "$REPO_ROOT" rev-parse --show-toplevel 2>/dev/null)
fi
REPO_NAME=$(basename "$REPO_ROOT")
REPO_HASH=$(echo "$REPO_ROOT" | md5 -q 2>/dev/null || echo "$REPO_NAME")

# Session ID: Use Claude's session or terminal PID as fallback
# CLAUDE_SESSION_ID is set by Claude Code, or use parent PID
SESSION_ID="${CLAUDE_SESSION_ID:-$(ps -o ppid= -p $$ 2>/dev/null | tr -d ' ')}"
SESSION_HASH=$(echo "${REPO_ROOT}_${SESSION_ID}" | md5 -q 2>/dev/null || echo "${REPO_HASH}_${SESSION_ID}")

# Per-session memory file (in repo's .claude/sessions directory)
SESSION_DIR="$REPO_ROOT/.claude/sessions"
mkdir -p "$SESSION_DIR" 2>/dev/null
MEMORY_FILE="${CLAUDE_MEMORY_FILE:-$SESSION_DIR/memory-${SESSION_HASH:0:8}.md}"

# Shared decisions file (all sessions in repo share decisions)
SHARED_DECISIONS="$REPO_ROOT/.claude/decisions.md"

# Per-session task marker
TASK_MARKER="$HOME/.claude/.task_active_${SESSION_HASH:0:16}"

# Context percentage is INFORMATIONAL ONLY - not a decision driver
# Decisions are based on task state and work status, not arbitrary thresholds
CONTEXT_INFO_THRESHOLD=50   # Show percentage in statusline when above this

# Ensure memory file exists
init_memory() {
    mkdir -p "$(dirname "$MEMORY_FILE")"
    if [[ ! -f "$MEMORY_FILE" ]]; then
        cat > "$MEMORY_FILE" << 'EOF'
# Session Memory
Updated: $(date -Iseconds)
TaskActive: false
ContextLevel: 0
Recommendation: CLEAR_OK

## Status
<!-- CLEAR_OK = All work committed, no pending tasks -->
<!-- HANDOVER = Generate detailed handover report before clearing -->
<!-- CONTINUE = Task in progress, allow context overflow if needed -->

## Decisions

## Pending

## Context
- Branch: unknown
- LastFiles:
- UncommittedFiles: 0

## Notes
EOF
    fi
}

# Check if task is active
is_task_active() {
    if [[ -f "$TASK_MARKER" ]]; then
        # Check if marker is recent (within 2 hours)
        marker_age=$(($(date +%s) - $(stat -f %m "$TASK_MARKER" 2>/dev/null || echo 0)))
        if [[ $marker_age -lt 7200 ]]; then
            echo "true"
            return 0
        else
            rm -f "$TASK_MARKER"
        fi
    fi
    echo "false"
    return 1
}

# Get context level (0-100 percentage estimate)
get_context_level() {
    local transcript_path="$1"
    local level=0

    if [[ -f "$transcript_path" ]]; then
        local size=$(wc -c < "$transcript_path" 2>/dev/null || echo "0")
        # Rough estimation: 2MB = 100%
        level=$((size * 100 / 2000000))
        [[ $level -gt 100 ]] && level=100
    fi

    echo "$level"
}

# Get uncommitted file count
get_uncommitted_count() {
    local cwd="${1:-.}"
    if git -C "$cwd" rev-parse --git-dir > /dev/null 2>&1; then
        git -C "$cwd" status --porcelain 2>/dev/null | wc -l | tr -d ' '
    else
        echo "0"
    fi
}

# Calculate recommendation - REQUIREMENTS-DRIVEN, not percentage-driven
# Decision tree:
#   1. Task in progress? → CONTINUE (never interrupt)
#   2. Uncommitted work? → HANDOVER (generate handover report)
#   3. Pending items? → HANDOVER (generate handover report)
#   4. High context (≥80%)? → HANDOVER (generate handover report)
#   5. Otherwise → CLEAR_OK (safe to start fresh)
get_recommendation() {
    local context_level="$1"
    local task_active="$2"
    local uncommitted="$3"

    # Priority 1: Task in progress - NEVER interrupt
    if [[ "$task_active" == "true" ]]; then
        echo "CONTINUE"
        return
    fi

    # Priority 2: Uncommitted work - generate handover
    if [[ $uncommitted -gt 0 ]]; then
        echo "HANDOVER"
        return
    fi

    # Priority 3: Check for pending items in memory
    if [[ -f "$MEMORY_FILE" ]]; then
        local pending_count=$(grep -c "^\- \[ \]" "$MEMORY_FILE" 2>/dev/null || echo "0")
        if [[ $pending_count -gt 0 ]]; then
            echo "HANDOVER"
            return
        fi
    fi

    # Priority 4: High context level - generate handover
    if [[ $context_level -ge 80 ]]; then
        echo "HANDOVER"
        return
    fi

    # No blockers - safe to clear
    echo "CLEAR_OK"
}

# Get status indicator for statusline
# New format: Show percentage → HANDOVER → CLEAR
# - <60%: Show percentage only (or nothing if <30%)
# - 60-79%: Show percentage
# - HANDOVER: Show "HANDOVER" (detailed summary ready)
# - CLEAR: Show "CLEAR" (safe to clear context)
# - CONTINUE: Show task with percentage
get_status_indicator() {
    local recommendation="$1"
    local task_active="$2"
    local context_level="$3"
    local uncommitted="$4"

    case "$recommendation" in
        "CLEAR_OK")
            # Safe to clear - show CLEAR indicator
            if [[ $context_level -lt 30 ]]; then
                echo ""  # Very low context, no indicator needed
            else
                echo "CLEAR"
            fi
            ;;
        "HANDOVER")
            # Generate handover report
            echo "HANDOVER"
            ;;
        "CONTINUE")
            # Task in progress - show task with percentage
            local task_desc=""
            if [[ -f "$TASK_MARKER" ]]; then
                task_desc=$(cat "$TASK_MARKER" 2>/dev/null | cut -c1-15)
            fi
            if [[ -n "$task_desc" ]]; then
                echo "TASK:${task_desc}[${context_level}%]"
            else
                echo "TASK[${context_level}%]"
            fi
            ;;
        *)
            # Unknown state
            if [[ $context_level -ge 60 ]]; then
                echo "${context_level}%"
            else
                echo ""
            fi
            ;;
    esac
}

# Update memory file
update_memory() {
    local context_level="$1"
    local task_active="$2"
    local recommendation="$3"
    local cwd="${4:-.}"

    local branch="unknown"
    local uncommitted=0

    if git -C "$cwd" rev-parse --git-dir > /dev/null 2>&1; then
        branch=$(git -C "$cwd" branch --show-current 2>/dev/null || echo "detached")
        uncommitted=$(get_uncommitted_count "$cwd")
    fi

    # Read existing decisions and pending
    local decisions=""
    local pending=""
    local notes=""
    local in_section=""

    while IFS= read -r line; do
        case "$line" in
            "## Decisions"*) in_section="decisions" ;;
            "## Pending"*) in_section="pending" ;;
            "## Context"*) in_section="" ;;
            "## Notes"*) in_section="notes" ;;
            "## "*) in_section="" ;;
            *)
                case "$in_section" in
                    "decisions") [[ "$line" =~ ^- ]] && decisions+="$line"$'\n' ;;
                    "pending") [[ "$line" =~ ^- ]] && pending+="$line"$'\n' ;;
                    "notes") [[ -n "$line" ]] && notes+="$line"$'\n' ;;
                esac
                ;;
        esac
    done < "$MEMORY_FILE"

    # Write updated memory
    cat > "$MEMORY_FILE" << EOF
# Session Memory
Updated: $(date -Iseconds)
TaskActive: $task_active
ContextLevel: $context_level
Recommendation: $recommendation

## Status
<!-- CLEAR_OK = All work committed, no pending tasks -->
<!-- HANDOVER = Generate detailed handover report before clearing -->
<!-- CONTINUE = Task in progress, allow context overflow if needed -->

## Decisions
$decisions
## Pending
$pending
## Context
- Branch: $branch
- LastFiles:
- UncommittedFiles: $uncommitted

## Notes
$notes
EOF
}

# Generate handover report for next session
generate_handover_report() {
    local cwd="${1:-.}"
    local handover_file="$SESSION_DIR/handover-$(date +%Y%m%d-%H%M%S).md"

    local branch="unknown"
    local uncommitted=0
    local recent_commits=""

    if git -C "$cwd" rev-parse --git-dir > /dev/null 2>&1; then
        branch=$(git -C "$cwd" branch --show-current 2>/dev/null || echo "detached")
        uncommitted=$(get_uncommitted_count "$cwd")
        recent_commits=$(git -C "$cwd" log --oneline -5 2>/dev/null || echo "No commits")
    fi

    # Read decisions and pending from memory
    local decisions=""
    local pending=""
    local notes=""

    if [[ -f "$MEMORY_FILE" ]]; then
        local in_section=""
        while IFS= read -r line; do
            case "$line" in
                "## Decisions"*) in_section="decisions" ;;
                "## Pending"*) in_section="pending" ;;
                "## Notes"*) in_section="notes" ;;
                "## "*) in_section="" ;;
                *)
                    case "$in_section" in
                        "decisions") [[ "$line" =~ ^- ]] && decisions+="$line"$'\n' ;;
                        "pending") [[ "$line" =~ ^- ]] && pending+="$line"$'\n' ;;
                        "notes") [[ -n "$line" ]] && notes+="$line"$'\n' ;;
                    esac
                    ;;
            esac
        done < "$MEMORY_FILE"
    fi

    # Generate handover report
    cat > "$handover_file" << EOF
# Session Handover - $(date -Iseconds)

## Repository
- Path: $cwd
- Branch: $branch
- Uncommitted files: $uncommitted

## Recent Commits
\`\`\`
$recent_commits
\`\`\`

## Decisions Made
$decisions

## Pending Tasks
$pending

## Notes for Next Session
$notes

---
Generated by memory-manager.sh
EOF

    echo "Handover report saved: $handover_file"
    echo "Review and then run: /clear"
}

# Mark task as started
start_task() {
    local description="$1"
    echo "$description" > "$TASK_MARKER"
    echo "Task started: $description"
}

# Mark task as ended
end_task() {
    rm -f "$TASK_MARKER"
    echo "Task ended"
}

# Add a decision (shared across all sessions in repo)
add_decision() {
    local decision="$1"
    local date_str=$(date +%m-%d)

    # Initialize shared decisions file if needed
    if [[ ! -f "$SHARED_DECISIONS" ]]; then
        cat > "$SHARED_DECISIONS" << 'EOF'
# Shared Decisions
<!-- Decisions shared across all sessions in this repo -->

EOF
    fi

    # Append to shared decisions file
    echo "- $decision ($date_str)" >> "$SHARED_DECISIONS"

    # Also add to session memory for context
    sed -i '' "/^## Decisions/a\\
- $decision ($date_str)
" "$MEMORY_FILE" 2>/dev/null || true
    echo "Decision recorded (shared): $decision"
}

# List all active sessions in this repo
list_sessions() {
    echo "Active sessions in $REPO_NAME:"
    for marker in "$HOME/.claude/.task_active_"*; do
        if [[ -f "$marker" ]]; then
            local marker_name=$(basename "$marker")
            local marker_age=$(($(date +%s) - $(stat -f %m "$marker" 2>/dev/null || echo 0)))
            if [[ $marker_age -lt 7200 ]]; then
                local task=$(cat "$marker" 2>/dev/null)
                echo "  - ${marker_name#.task_active_}: $task (${marker_age}s ago)"
            fi
        fi
    done
}

# Cleanup old session files (older than 24 hours)
cleanup_sessions() {
    local count=0
    # Cleanup old memory files
    find "$SESSION_DIR" -name "memory-*.md" -mtime +1 -delete 2>/dev/null && count=$((count + 1))
    # Cleanup old task markers
    for marker in "$HOME/.claude/.task_active_"*; do
        if [[ -f "$marker" ]]; then
            local marker_age=$(($(date +%s) - $(stat -f %m "$marker" 2>/dev/null || echo 0)))
            if [[ $marker_age -gt 86400 ]]; then
                rm -f "$marker"
                count=$((count + 1))
            fi
        fi
    done
    echo "Cleaned up $count old session files"
}

# Main command handler
main() {
    init_memory

    local cmd="${1:-check}"
    local cwd="${CWD:-.}"
    local transcript_path="${TRANSCRIPT_PATH:-}"

    case "$cmd" in
        "check")
            local context_level=$(get_context_level "$transcript_path")
            local task_active=$(is_task_active)
            local uncommitted=$(get_uncommitted_count "$cwd")
            local recommendation=$(get_recommendation "$context_level" "$task_active" "$uncommitted")
            local indicator=$(get_status_indicator "$recommendation" "$task_active" "$context_level" "$uncommitted")

            # Update memory with current state
            update_memory "$context_level" "$task_active" "$recommendation" "$cwd"

            # Output for statusline (just the indicator)
            if [[ "${OUTPUT_FORMAT:-}" == "statusline" ]]; then
                echo "$indicator"
            else
                # Full output
                echo "Context: ${context_level}%"
                echo "Task Active: $task_active"
                echo "Uncommitted: $uncommitted"
                echo "Recommendation: $recommendation"
                echo "Indicator: $indicator"

                case "$recommendation" in
                    "CLEAR_OK")
                        echo ""
                        echo "Status: Safe to /clear - no pending work"
                        ;;
                    "HANDOVER")
                        echo ""
                        if [[ $uncommitted -gt 0 ]]; then
                            echo "Status: $uncommitted uncommitted files - commit first, then generate handover report"
                        else
                            echo "Status: Ready for handover - generate detailed summary, then /clear"
                        fi
                        ;;
                    "CONTINUE")
                        echo ""
                        echo "Status: Task in progress - continue working"
                        if [[ -f "$TASK_MARKER" ]]; then
                            echo "Task: $(cat "$TASK_MARKER")"
                        fi
                        ;;
                esac
            fi
            ;;
        "save")
            local context_level=$(get_context_level "$transcript_path")
            local task_active=$(is_task_active)
            local uncommitted=$(get_uncommitted_count "$cwd")
            local recommendation=$(get_recommendation "$context_level" "$task_active" "$uncommitted")
            update_memory "$context_level" "$task_active" "$recommendation" "$cwd"
            echo "Memory saved"
            ;;
        "load")
            cat "$MEMORY_FILE"
            ;;
        "task-start")
            start_task "${2:-unknown task}"
            ;;
        "task-end")
            end_task
            ;;
        "decision")
            add_decision "${2:-unknown decision}"
            ;;
        "sessions")
            list_sessions
            ;;
        "handover")
            generate_handover_report "$cwd"
            ;;
        "cleanup")
            cleanup_sessions
            ;;
        *)
            echo "Usage: memory-manager.sh {check|save|load|task-start|task-end|decision|handover|sessions|cleanup}"
            exit 1
            ;;
    esac
}

main "$@"
