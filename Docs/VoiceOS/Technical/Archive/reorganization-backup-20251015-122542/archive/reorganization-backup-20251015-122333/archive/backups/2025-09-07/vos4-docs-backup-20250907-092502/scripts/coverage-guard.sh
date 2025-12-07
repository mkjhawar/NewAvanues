#!/bin/bash

# VOS4 Coverage Guard - Test Coverage Enforcement System
# 
# Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
# Created: 2025-01-02
# 
# Features:
# - Pre-merge coverage validation
# - Baseline coverage comparison
# - Detailed coverage reporting
# - Git hook integration
# - Notification system

set -euo pipefail

# ========================================
# Configuration
# ========================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BUILD_DIR="$PROJECT_ROOT/build"
REPORTS_DIR="$BUILD_DIR/coverage-reports"
BASELINE_FILE="$REPORTS_DIR/coverage-baseline.json"
CURRENT_FILE="$REPORTS_DIR/coverage-current.json"
HTML_REPORT="$REPORTS_DIR/coverage-report.html"

# Coverage thresholds (can be overridden by environment variables)
MIN_TOTAL_COVERAGE=${MIN_TOTAL_COVERAGE:-85.0}
MIN_MODULE_COVERAGE=${MIN_MODULE_COVERAGE:-80.0}
MAX_COVERAGE_DROP=${MAX_COVERAGE_DROP:-2.0}

# Notification settings
SLACK_WEBHOOK_URL=${SLACK_WEBHOOK_URL:-""}
NOTIFICATION_EMAIL=${NOTIFICATION_EMAIL:-""}
NOTIFICATION_ENABLED=${NOTIFICATION_ENABLED:-"true"}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# ========================================
# Utility Functions
# ========================================

log() {
    echo -e "${CYAN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

success() {
    echo -e "${GREEN}✅ $1${NC}"
}

warning() {
    echo -e "${YELLOW}⚠️ $1${NC}"
}

error() {
    echo -e "${RED}❌ $1${NC}"
}

info() {
    echo -e "${BLUE}ℹ️ $1${NC}"
}

progress() {
    echo -e "${PURPLE}🔄 $1${NC}"
}

# ========================================
# Coverage Analysis Functions
# ========================================

extract_jacoco_metrics() {
    local xml_file="$1"
    local module="$2"
    
    if [[ ! -f "$xml_file" ]]; then
        warning "JaCoCo XML report not found: $xml_file"
        return 1
    fi
    
    # Extract coverage metrics using xmllint if available, otherwise use sed/grep
    if command -v xmllint >/dev/null 2>&1; then
        extract_with_xmllint "$xml_file" "$module"
    else
        extract_with_sed "$xml_file" "$module"
    fi
}

extract_with_xmllint() {
    local xml_file="$1"
    local module="$2"
    
    local line_covered=$(xmllint --xpath "//counter[@type='LINE']/@covered" "$xml_file" 2>/dev/null | cut -d'"' -f2)
    local line_missed=$(xmllint --xpath "//counter[@type='LINE']/@missed" "$xml_file" 2>/dev/null | cut -d'"' -f2)
    local branch_covered=$(xmllint --xpath "//counter[@type='BRANCH']/@covered" "$xml_file" 2>/dev/null | cut -d'"' -f2)
    local branch_missed=$(xmllint --xpath "//counter[@type='BRANCH']/@missed" "$xml_file" 2>/dev/null | cut -d'"' -f2)
    local method_covered=$(xmllint --xpath "//counter[@type='METHOD']/@covered" "$xml_file" 2>/dev/null | cut -d'"' -f2)
    local method_missed=$(xmllint --xpath "//counter[@type='METHOD']/@missed" "$xml_file" 2>/dev/null | cut -d'"' -f2)
    local class_covered=$(xmllint --xpath "//counter[@type='CLASS']/@covered" "$xml_file" 2>/dev/null | cut -d'"' -f2)
    local class_missed=$(xmllint --xpath "//counter[@type='CLASS']/@missed" "$xml_file" 2>/dev/null | cut -d'"' -f2)
    
    calculate_coverage_percentages "$module" "$line_covered" "$line_missed" \
        "$branch_covered" "$branch_missed" "$method_covered" "$method_missed" \
        "$class_covered" "$class_missed"
}

extract_with_sed() {
    local xml_file="$1"
    local module="$2"
    
    local line_metrics=$(grep '<counter type="LINE"' "$xml_file" | head -1)
    local branch_metrics=$(grep '<counter type="BRANCH"' "$xml_file" | head -1)
    local method_metrics=$(grep '<counter type="METHOD"' "$xml_file" | head -1)
    local class_metrics=$(grep '<counter type="CLASS"' "$xml_file" | head -1)
    
    local line_covered=$(echo "$line_metrics" | sed -n 's/.*covered="\([^"]*\)".*/\1/p')
    local line_missed=$(echo "$line_metrics" | sed -n 's/.*missed="\([^"]*\)".*/\1/p')
    local branch_covered=$(echo "$branch_metrics" | sed -n 's/.*covered="\([^"]*\)".*/\1/p')
    local branch_missed=$(echo "$branch_metrics" | sed -n 's/.*missed="\([^"]*\)".*/\1/p')
    local method_covered=$(echo "$method_metrics" | sed -n 's/.*covered="\([^"]*\)".*/\1/p')
    local method_missed=$(echo "$method_metrics" | sed -n 's/.*missed="\([^"]*\)".*/\1/p')
    local class_covered=$(echo "$class_metrics" | sed -n 's/.*covered="\([^"]*\)".*/\1/p')
    local class_missed=$(echo "$class_metrics" | sed -n 's/.*missed="\([^"]*\)".*/\1/p')
    
    calculate_coverage_percentages "$module" "${line_covered:-0}" "${line_missed:-0}" \
        "${branch_covered:-0}" "${branch_missed:-0}" "${method_covered:-0}" "${method_missed:-0}" \
        "${class_covered:-0}" "${class_missed:-0}"
}

calculate_coverage_percentages() {
    local module="$1"
    local line_covered="$2"
    local line_missed="$3"
    local branch_covered="$4"
    local branch_missed="$5"
    local method_covered="$6"
    local method_missed="$7"
    local class_covered="$8"
    local class_missed="$9"
    
    # Calculate percentages using bc if available, otherwise use awk
    if command -v bc >/dev/null 2>&1; then
        local line_total=$((line_covered + line_missed))
        local branch_total=$((branch_covered + branch_missed))
        local method_total=$((method_covered + method_missed))
        local class_total=$((class_covered + class_missed))
        
        local line_pct=$(echo "scale=2; if ($line_total > 0) $line_covered * 100 / $line_total else 0" | bc -l)
        local branch_pct=$(echo "scale=2; if ($branch_total > 0) $branch_covered * 100 / $branch_total else 0" | bc -l)
        local method_pct=$(echo "scale=2; if ($method_total > 0) $method_covered * 100 / $method_total else 0" | bc -l)
        local class_pct=$(echo "scale=2; if ($class_total > 0) $class_covered * 100 / $class_total else 0" | bc -l)
    else
        # Fallback to awk for percentage calculations
        local line_pct=$(awk "BEGIN {total=$line_covered+$line_missed; print (total > 0) ? ($line_covered*100/total) : 0}")
        local branch_pct=$(awk "BEGIN {total=$branch_covered+$branch_missed; print (total > 0) ? ($branch_covered*100/total) : 0}")
        local method_pct=$(awk "BEGIN {total=$method_covered+$method_missed; print (total > 0) ? ($method_covered*100/total) : 0}")
        local class_pct=$(awk "BEGIN {total=$class_covered+$class_missed; print (total > 0) ? ($class_covered*100/total) : 0}")
    fi
    
    # Store results in global associative array (or files for portability)
    echo "{
        \"module\": \"$module\",
        \"line_coverage\": $line_pct,
        \"branch_coverage\": $branch_pct,
        \"method_coverage\": $method_pct,
        \"class_coverage\": $class_pct,
        \"line_covered\": $line_covered,
        \"line_total\": $((line_covered + line_missed)),
        \"branch_covered\": $branch_covered,
        \"branch_total\": $((branch_covered + branch_missed)),
        \"method_covered\": $method_covered,
        \"method_total\": $((method_covered + method_missed)),
        \"class_covered\": $class_covered,
        \"class_total\": $((class_covered + class_missed)),
        \"timestamp\": $(date +%s)
    }" > "$REPORTS_DIR/coverage-$module.json"
    
    info "📊 $module Coverage: Line ${line_pct}%, Branch ${branch_pct}%, Method ${method_pct}%, Class ${class_pct}%"
}

# ========================================
# Coverage Collection Functions
# ========================================

collect_coverage_data() {
    progress "Collecting coverage data from all modules..."
    
    mkdir -p "$REPORTS_DIR"
    
    # Initialize current coverage file
    echo "{
        \"timestamp\": $(date +%s),
        \"commit\": \"$(git rev-parse HEAD 2>/dev/null || echo 'unknown')\",
        \"branch\": \"$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')\",
        \"modules\": {}
    }" > "$CURRENT_FILE"
    
    local modules_processed=0
    local total_line_covered=0
    local total_line_total=0
    local total_branch_covered=0
    local total_branch_total=0
    
    # Find all JaCoCo XML reports
    find "$PROJECT_ROOT" -name "jacocoTestReport.xml" -o -name "jacoco.xml" | while IFS= read -r xml_file; do
        local module=$(extract_module_name "$xml_file")
        if [[ -n "$module" ]]; then
            progress "Processing coverage for module: $module"
            extract_jacoco_metrics "$xml_file" "$module"
            
            # Read the generated JSON file to add to current coverage
            if [[ -f "$REPORTS_DIR/coverage-$module.json" ]]; then
                local module_data=$(cat "$REPORTS_DIR/coverage-$module.json")
                # Add to current file (simplified - in production would use jq)
                modules_processed=$((modules_processed + 1))
            fi
        fi
    done
    
    # Calculate overall coverage
    if [[ $modules_processed -gt 0 ]]; then
        calculate_total_coverage
        success "Coverage data collected for $modules_processed modules"
    else
        warning "No coverage reports found. Make sure to run tests with coverage first:"
        echo "  ./gradlew test jacocoTestReport"
        return 1
    fi
}

extract_module_name() {
    local xml_path="$1"
    
    # Extract module name from path
    if [[ "$xml_path" =~ /([^/]+)/build/reports/jacoco/ ]]; then
        echo "${BASH_REMATCH[1]}"
    elif [[ "$xml_path" =~ /apps/([^/]+)/ ]]; then
        echo "${BASH_REMATCH[1]}"
    elif [[ "$xml_path" =~ /libraries/([^/]+)/ ]]; then
        echo "${BASH_REMATCH[1]}"
    elif [[ "$xml_path" =~ /managers/([^/]+)/ ]]; then
        echo "${BASH_REMATCH[1]}"
    elif [[ "$xml_path" =~ /app/build/ ]]; then
        echo "app"
    else
        echo "unknown"
    fi
}

calculate_total_coverage() {
    local total_line_covered=0
    local total_line_total=0
    local total_branch_covered=0
    local total_branch_total=0
    local module_count=0
    
    for coverage_file in "$REPORTS_DIR"/coverage-*.json; do
        if [[ -f "$coverage_file" ]] && [[ "$coverage_file" != *"baseline"* ]] && [[ "$coverage_file" != *"current"* ]]; then
            local line_covered=$(grep '"line_covered"' "$coverage_file" | sed 's/.*: *\([0-9]*\).*/\1/')
            local line_total=$(grep '"line_total"' "$coverage_file" | sed 's/.*: *\([0-9]*\).*/\1/')
            local branch_covered=$(grep '"branch_covered"' "$coverage_file" | sed 's/.*: *\([0-9]*\).*/\1/')
            local branch_total=$(grep '"branch_total"' "$coverage_file" | sed 's/.*: *\([0-9]*\).*/\1/')
            
            total_line_covered=$((total_line_covered + line_covered))
            total_line_total=$((total_line_total + line_total))
            total_branch_covered=$((total_branch_covered + branch_covered))
            total_branch_total=$((total_branch_total + branch_total))
            module_count=$((module_count + 1))
        fi
    done
    
    if [[ $total_line_total -gt 0 ]]; then
        local overall_line_pct=$(awk "BEGIN {print ($total_line_covered*100/$total_line_total)}")
        local overall_branch_pct=$(awk "BEGIN {print (total_branch_total > 0) ? ($total_branch_covered*100/$total_branch_total) : 0}" total_branch_total=$total_branch_total)
        
        # Update current coverage file with totals
        local temp_file=$(mktemp)
        cat "$CURRENT_FILE" | sed "s/\"modules\": {}/\"modules\": {}, \"total_line_coverage\": $overall_line_pct, \"total_branch_coverage\": $overall_branch_pct/" > "$temp_file"
        mv "$temp_file" "$CURRENT_FILE"
        
        info "📈 Overall Coverage: Line ${overall_line_pct}%, Branch ${overall_branch_pct}%"
        
        # Store in global variables for later use
        CURRENT_TOTAL_COVERAGE="$overall_line_pct"
        export CURRENT_TOTAL_COVERAGE
    fi
}

# ========================================
# Baseline Comparison Functions
# ========================================

load_baseline_coverage() {
    if [[ -f "$BASELINE_FILE" ]]; then
        info "📋 Loading baseline coverage from $BASELINE_FILE"
        return 0
    else
        warning "No baseline coverage file found. Creating baseline from current coverage..."
        save_coverage_baseline
        return 1
    fi
}

save_coverage_baseline() {
    if [[ -f "$CURRENT_FILE" ]]; then
        cp "$CURRENT_FILE" "$BASELINE_FILE"
        success "💾 Coverage baseline saved to $BASELINE_FILE"
    else
        error "No current coverage data to save as baseline"
        return 1
    fi
}

compare_with_baseline() {
    if [[ ! -f "$BASELINE_FILE" ]]; then
        warning "No baseline found. Current coverage will become the baseline."
        save_coverage_baseline
        return 0
    fi
    
    progress "🔍 Comparing current coverage with baseline..."
    
    # Extract coverage values (simplified - would use jq in production)
    local current_coverage="${CURRENT_TOTAL_COVERAGE:-0}"
    local baseline_coverage=$(grep '"total_line_coverage"' "$BASELINE_FILE" 2>/dev/null | sed 's/.*: *\([0-9.]*\).*/\1/' || echo "0")
    
    if [[ -z "$baseline_coverage" ]] || [[ "$baseline_coverage" == "0" ]]; then
        warning "Could not extract baseline coverage. Skipping comparison."
        return 0
    fi
    
    local coverage_diff=$(awk "BEGIN {print ($current_coverage - $baseline_coverage)}")
    
    info "📊 Coverage Comparison:"
    info "   Current:  ${current_coverage}%"
    info "   Baseline: ${baseline_coverage}%"
    info "   Diff:     ${coverage_diff}%"
    
    # Check if coverage dropped significantly
    if (( $(echo "$coverage_diff < -$MAX_COVERAGE_DROP" | bc -l) )); then
        error "Coverage dropped by ${coverage_diff}% (threshold: -${MAX_COVERAGE_DROP}%)"
        COVERAGE_CHECK_FAILED="true"
        export COVERAGE_CHECK_FAILED
        
        # Generate detailed diff report
        generate_coverage_diff_report
        
        return 1
    else
        success "Coverage change within acceptable limits (${coverage_diff}%)"
        return 0
    fi
}

generate_coverage_diff_report() {
    local diff_report="$REPORTS_DIR/coverage-diff.txt"
    
    {
        echo "=========================================="
        echo "VOS4 Coverage Regression Report"
        echo "=========================================="
        echo "Date: $(date)"
        echo "Commit: $(git rev-parse HEAD 2>/dev/null || echo 'unknown')"
        echo "Branch: $(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')"
        echo ""
        echo "OVERALL COVERAGE DROP DETECTED"
        echo ""
        
        # Module-by-module comparison
        for coverage_file in "$REPORTS_DIR"/coverage-*.json; do
            if [[ -f "$coverage_file" ]] && [[ "$coverage_file" != *"baseline"* ]] && [[ "$coverage_file" != *"current"* ]]; then
                local module=$(basename "$coverage_file" .json | sed 's/coverage-//')
                local current_pct=$(grep '"line_coverage"' "$coverage_file" | sed 's/.*: *\([0-9.]*\).*/\1/')
                echo "Module: $module - ${current_pct}%"
            fi
        done
        
        echo ""
        echo "ACTION REQUIRED:"
        echo "1. Review test coverage for modules with drops"
        echo "2. Add missing tests before merging"
        echo "3. Update baseline after coverage improvements"
        
    } > "$diff_report"
    
    info "📄 Detailed diff report saved to $diff_report"
}

# ========================================
# Validation Functions
# ========================================

validate_coverage_thresholds() {
    progress "🎯 Validating coverage against thresholds..."
    
    local validation_failed=false
    local current_coverage="${CURRENT_TOTAL_COVERAGE:-0}"
    
    # Check overall coverage threshold
    if (( $(echo "$current_coverage < $MIN_TOTAL_COVERAGE" | bc -l) )); then
        error "Overall coverage ${current_coverage}% is below minimum threshold ${MIN_TOTAL_COVERAGE}%"
        validation_failed=true
    else
        success "Overall coverage ${current_coverage}% meets minimum threshold ${MIN_TOTAL_COVERAGE}%"
    fi
    
    # Check individual module thresholds
    for coverage_file in "$REPORTS_DIR"/coverage-*.json; do
        if [[ -f "$coverage_file" ]] && [[ "$coverage_file" != *"baseline"* ]] && [[ "$coverage_file" != *"current"* ]]; then
            local module=$(basename "$coverage_file" .json | sed 's/coverage-//')
            local module_coverage=$(grep '"line_coverage"' "$coverage_file" | sed 's/.*: *\([0-9.]*\).*/\1/')
            
            if (( $(echo "$module_coverage < $MIN_MODULE_COVERAGE" | bc -l) )); then
                error "Module $module coverage ${module_coverage}% is below minimum threshold ${MIN_MODULE_COVERAGE}%"
                validation_failed=true
            else
                success "Module $module coverage ${module_coverage}% meets minimum threshold"
            fi
        fi
    done
    
    if [[ "$validation_failed" == "true" ]]; then
        COVERAGE_CHECK_FAILED="true"
        export COVERAGE_CHECK_FAILED
        return 1
    fi
    
    return 0
}

# ========================================
# Report Generation Functions
# ========================================

generate_html_report() {
    progress "📊 Generating HTML coverage report..."
    
    cat > "$HTML_REPORT" << 'EOF'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>VOS4 Coverage Report</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            color: #333;
        }
        
        .header {
            background: rgba(255, 255, 255, 0.1);
            backdrop-filter: blur(10px);
            padding: 2rem;
            text-align: center;
            border-bottom: 1px solid rgba(255, 255, 255, 0.2);
        }
        
        .header h1 {
            color: white;
            font-size: 2.5rem;
            margin-bottom: 0.5rem;
        }
        
        .header p {
            color: rgba(255, 255, 255, 0.8);
            font-size: 1.1rem;
        }
        
        .container {
            max-width: 1200px;
            margin: 2rem auto;
            padding: 0 1rem;
        }
        
        .summary-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 1.5rem;
            margin-bottom: 2rem;
        }
        
        .metric-card {
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(10px);
            border-radius: 12px;
            padding: 1.5rem;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
            text-align: center;
        }
        
        .metric-value {
            font-size: 2.5rem;
            font-weight: 700;
            margin-bottom: 0.5rem;
        }
        
        .metric-label {
            font-size: 0.9rem;
            color: #666;
            text-transform: uppercase;
            letter-spacing: 1px;
        }
        
        .coverage-good { color: #48bb78; }
        .coverage-warning { color: #ed8936; }
        .coverage-danger { color: #f56565; }
        
        .modules-table {
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(10px);
            border-radius: 12px;
            overflow: hidden;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
        }
        
        .table-header {
            background: #4299e1;
            color: white;
            padding: 1rem;
            font-weight: 600;
        }
        
        table {
            width: 100%;
            border-collapse: collapse;
        }
        
        th, td {
            padding: 1rem;
            text-align: left;
            border-bottom: 1px solid #e2e8f0;
        }
        
        th {
            background: #f7fafc;
            font-weight: 600;
            color: #2d3748;
        }
        
        .progress-bar {
            width: 100%;
            height: 8px;
            background: #e2e8f0;
            border-radius: 4px;
            overflow: hidden;
        }
        
        .progress-fill {
            height: 100%;
            transition: width 0.3s ease;
        }
        
        .status-badge {
            padding: 0.25rem 0.75rem;
            border-radius: 20px;
            font-size: 0.75rem;
            font-weight: 600;
            text-transform: uppercase;
        }
        
        .status-pass {
            background: #c6f6d5;
            color: #22543d;
        }
        
        .status-warning {
            background: #feebc8;
            color: #c05621;
        }
        
        .status-fail {
            background: #fed7d7;
            color: #c53030;
        }
        
        .footer {
            text-align: center;
            padding: 2rem;
            color: rgba(255, 255, 255, 0.8);
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>🛡️ VOS4 Coverage Guard Report</h1>
        <p>Generated on $(date)</p>
    </div>
    
    <div class="container">
        <div class="summary-grid">
EOF

    # Add current coverage metrics to HTML
    local current_coverage="${CURRENT_TOTAL_COVERAGE:-0}"
    local status_class="coverage-good"
    
    if (( $(echo "$current_coverage < $MIN_TOTAL_COVERAGE" | bc -l) )); then
        status_class="coverage-danger"
    elif (( $(echo "$current_coverage < 90" | bc -l) )); then
        status_class="coverage-warning"
    fi
    
    cat >> "$HTML_REPORT" << EOF
            <div class="metric-card">
                <div class="metric-value $status_class">${current_coverage}%</div>
                <div class="metric-label">Overall Coverage</div>
            </div>
            
            <div class="metric-card">
                <div class="metric-value coverage-good">$(find "$PROJECT_ROOT" -name "*.kt" -type f | wc -l | xargs)</div>
                <div class="metric-label">Source Files</div>
            </div>
            
            <div class="metric-card">
                <div class="metric-value coverage-good">$(git rev-parse --short HEAD 2>/dev/null || echo 'N/A')</div>
                <div class="metric-label">Commit</div>
            </div>
        </div>
        
        <div class="modules-table">
            <div class="table-header">
                <h2>📦 Module Coverage Details</h2>
            </div>
            <table>
                <thead>
                    <tr>
                        <th>Module</th>
                        <th>Line Coverage</th>
                        <th>Branch Coverage</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
EOF

    # Add module rows
    for coverage_file in "$REPORTS_DIR"/coverage-*.json; do
        if [[ -f "$coverage_file" ]] && [[ "$coverage_file" != *"baseline"* ]] && [[ "$coverage_file" != *"current"* ]]; then
            local module=$(basename "$coverage_file" .json | sed 's/coverage-//')
            local line_coverage=$(grep '"line_coverage"' "$coverage_file" | sed 's/.*: *\([0-9.]*\).*/\1/')
            local branch_coverage=$(grep '"branch_coverage"' "$coverage_file" | sed 's/.*: *\([0-9.]*\).*/\1/')
            
            local status="status-pass"
            local status_text="PASS"
            
            if (( $(echo "$line_coverage < $MIN_MODULE_COVERAGE" | bc -l) )); then
                status="status-fail"
                status_text="FAIL"
            elif (( $(echo "$line_coverage < 90" | bc -l) )); then
                status="status-warning"
                status_text="WARN"
            fi
            
            cat >> "$HTML_REPORT" << EOF
                    <tr>
                        <td><strong>$module</strong></td>
                        <td>
                            <div>${line_coverage}%</div>
                            <div class="progress-bar">
                                <div class="progress-fill coverage-good" style="width: ${line_coverage}%"></div>
                            </div>
                        </td>
                        <td>
                            <div>${branch_coverage}%</div>
                            <div class="progress-bar">
                                <div class="progress-fill coverage-good" style="width: ${branch_coverage}%"></div>
                            </div>
                        </td>
                        <td><span class="status-badge $status">$status_text</span></td>
                    </tr>
EOF
        fi
    done

    cat >> "$HTML_REPORT" << 'EOF'
                </tbody>
            </table>
        </div>
    </div>
    
    <div class="footer">
        <p>🧪 VOS4 Coverage Guard - Ensuring Code Quality</p>
    </div>
</body>
</html>
EOF

    success "📊 HTML report generated: $HTML_REPORT"
}

# ========================================
# Notification Functions
# ========================================

send_slack_notification() {
    local message="$1"
    local color="$2" # good, warning, danger
    
    if [[ -z "$SLACK_WEBHOOK_URL" ]] || [[ "$NOTIFICATION_ENABLED" != "true" ]]; then
        return 0
    fi
    
    local payload=$(cat << EOF
{
    "attachments": [
        {
            "color": "$color",
            "title": "VOS4 Coverage Guard Alert",
            "text": "$message",
            "footer": "VOS4 Coverage Guard",
            "ts": $(date +%s)
        }
    ]
}
EOF
)
    
    if curl -s -X POST -H 'Content-type: application/json' --data "$payload" "$SLACK_WEBHOOK_URL" >/dev/null; then
        success "📱 Slack notification sent"
    else
        warning "Failed to send Slack notification"
    fi
}

send_email_notification() {
    local subject="$1"
    local body="$2"
    
    if [[ -z "$NOTIFICATION_EMAIL" ]] || [[ "$NOTIFICATION_ENABLED" != "true" ]]; then
        return 0
    fi
    
    if command -v mail >/dev/null 2>&1; then
        echo "$body" | mail -s "$subject" "$NOTIFICATION_EMAIL"
        success "📧 Email notification sent to $NOTIFICATION_EMAIL"
    else
        warning "Mail command not available. Skipping email notification."
    fi
}

notify_coverage_status() {
    local status="$1"
    local coverage="${CURRENT_TOTAL_COVERAGE:-0}"
    
    case "$status" in
        "pass")
            send_slack_notification "✅ Coverage check passed! Current coverage: ${coverage}% (threshold: ${MIN_TOTAL_COVERAGE}%)" "good"
            ;;
        "fail")
            local message="❌ Coverage check failed! Current coverage: ${coverage}% is below threshold: ${MIN_TOTAL_COVERAGE}%"
            send_slack_notification "$message" "danger"
            send_email_notification "VOS4 Coverage Check Failed" "$message"
            ;;
        "regression")
            local message="⚠️ Coverage regression detected! Please review changes and add tests."
            send_slack_notification "$message" "warning"
            ;;
    esac
}

# ========================================
# Git Integration Functions
# ========================================

install_git_hooks() {
    progress "🔧 Installing Git hooks..."
    
    local hooks_dir="$PROJECT_ROOT/.git/hooks"
    local pre_commit_hook="$hooks_dir/pre-commit"
    local pre_push_hook="$hooks_dir/pre-push"
    
    if [[ ! -d "$hooks_dir" ]]; then
        error "Git hooks directory not found. Are you in a Git repository?"
        return 1
    fi
    
    # Create pre-commit hook
    cat > "$pre_commit_hook" << 'EOF'
#!/bin/bash
# VOS4 Coverage Guard - Pre-commit Hook

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

echo "🛡️ Running VOS4 Coverage Guard pre-commit check..."

# Run tests and generate coverage
cd "$PROJECT_ROOT"
./gradlew test jacocoTestReport --continue

# Run coverage guard
if ./scripts/coverage-guard.sh --check; then
    echo "✅ Coverage check passed"
    exit 0
else
    echo "❌ Coverage check failed"
    echo ""
    echo "To bypass this check (not recommended):"
    echo "  git commit --no-verify"
    echo ""
    echo "To fix coverage issues:"
    echo "  1. Add missing tests"
    echo "  2. Run: ./gradlew test jacocoTestReport"
    echo "  3. Run: ./scripts/coverage-guard.sh --check"
    exit 1
fi
EOF

    chmod +x "$pre_commit_hook"
    
    # Create pre-push hook
    cat > "$pre_push_hook" << 'EOF'
#!/bin/bash
# VOS4 Coverage Guard - Pre-push Hook

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

echo "🛡️ Running VOS4 Coverage Guard pre-push validation..."

cd "$PROJECT_ROOT"
./gradlew test jacocoTestReport --continue

if ./scripts/coverage-guard.sh --validate; then
    echo "✅ Coverage validation passed"
    exit 0
else
    echo "❌ Coverage validation failed"
    echo ""
    echo "Push blocked due to coverage issues."
    echo "Please fix coverage issues before pushing."
    exit 1
fi
EOF

    chmod +x "$pre_push_hook"
    
    success "🔧 Git hooks installed successfully"
    info "   Pre-commit: $pre_commit_hook"
    info "   Pre-push: $pre_push_hook"
}

# ========================================
# Main Functions
# ========================================

run_coverage_check() {
    log "🛡️ Starting VOS4 Coverage Guard..."
    
    # Collect coverage data
    if ! collect_coverage_data; then
        error "Failed to collect coverage data"
        return 1
    fi
    
    # Load baseline and compare
    load_baseline_coverage
    local baseline_comparison_result=0
    if ! compare_with_baseline; then
        baseline_comparison_result=1
    fi
    
    # Validate against thresholds
    local threshold_validation_result=0
    if ! validate_coverage_thresholds; then
        threshold_validation_result=1
    fi
    
    # Generate reports
    generate_html_report
    
    # Determine overall result
    if [[ "$threshold_validation_result" -eq 0 ]] && [[ "$baseline_comparison_result" -eq 0 ]]; then
        success "🎉 Coverage guard check passed!"
        notify_coverage_status "pass"
        return 0
    else
        if [[ "$threshold_validation_result" -ne 0 ]]; then
            notify_coverage_status "fail"
        elif [[ "$baseline_comparison_result" -ne 0 ]]; then
            notify_coverage_status "regression"
        fi
        
        error "Coverage guard check failed!"
        info "📊 View detailed report: file://$HTML_REPORT"
        return 1
    fi
}

show_help() {
    cat << 'EOF'
🛡️ VOS4 Coverage Guard - Test Coverage Enforcement System

USAGE:
    ./coverage-guard.sh [OPTIONS]

OPTIONS:
    --check              Run coverage check (default action)
    --validate           Run full validation (check + baseline comparison)
    --baseline           Save current coverage as new baseline
    --report             Generate HTML report only
    --install-hooks      Install Git pre-commit/pre-push hooks
    --help               Show this help message

ENVIRONMENT VARIABLES:
    MIN_TOTAL_COVERAGE      Minimum total coverage percentage (default: 85.0)
    MIN_MODULE_COVERAGE     Minimum per-module coverage (default: 80.0)
    MAX_COVERAGE_DROP       Maximum allowed coverage drop (default: 2.0)
    SLACK_WEBHOOK_URL       Slack webhook for notifications
    NOTIFICATION_EMAIL      Email for notifications
    NOTIFICATION_ENABLED    Enable notifications (default: true)

EXAMPLES:
    # Run coverage check
    ./coverage-guard.sh --check

    # Save current coverage as baseline
    ./coverage-guard.sh --baseline

    # Install Git hooks
    ./coverage-guard.sh --install-hooks

    # Generate HTML report only
    ./coverage-guard.sh --report

    # Run with custom thresholds
    MIN_TOTAL_COVERAGE=90 ./coverage-guard.sh --validate

INTEGRATION:
    # Add to CI/CD pipeline
    ./gradlew test jacocoTestReport
    ./scripts/coverage-guard.sh --validate

    # Pre-merge validation
    git add . && git commit  # Will trigger pre-commit hook
    git push                 # Will trigger pre-push hook

EOF
}

# ========================================
# Main Entry Point
# ========================================

main() {
    local action="check"
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --check)
                action="check"
                shift
                ;;
            --validate)
                action="validate"
                shift
                ;;
            --baseline)
                action="baseline"
                shift
                ;;
            --report)
                action="report"
                shift
                ;;
            --install-hooks)
                action="install-hooks"
                shift
                ;;
            --help|-h)
                show_help
                exit 0
                ;;
            *)
                error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # Ensure we're in project root
    if [[ ! -f "$PROJECT_ROOT/build.gradle.kts" ]]; then
        error "Must be run from VOS4 project root directory"
        exit 1
    fi
    
    # Execute requested action
    case "$action" in
        "check"|"validate")
            run_coverage_check
            exit $?
            ;;
        "baseline")
            if collect_coverage_data; then
                save_coverage_baseline
                success "✅ Baseline updated successfully"
                exit 0
            else
                error "Failed to collect coverage data for baseline"
                exit 1
            fi
            ;;
        "report")
            if collect_coverage_data; then
                generate_html_report
                info "📊 View report: file://$HTML_REPORT"
                exit 0
            else
                error "Failed to generate report"
                exit 1
            fi
            ;;
        "install-hooks")
            install_git_hooks
            exit $?
            ;;
    esac
}

# Run main function with all arguments
main "$@"