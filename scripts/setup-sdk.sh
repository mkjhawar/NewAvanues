#!/usr/bin/env bash
#
# setup-sdk.sh — Download large binaries (>100MB) from GitLab Package Registry
#
# Policy: Files <100MB per file are tracked in git (GitLab blob limit).
# Only files exceeding 100MB are downloaded from the Package Registry.
# Run this after cloning.
#
# What gets downloaded:
#   VLM Base models  (~141MB x4 = 564MB)  → VLMFiles/{EN,MUL}/VoiceOS-Bas-*
#   VLM Small models (~465MB x4 = 1.9GB)  → VLMFiles/{EN,MUL}/VoiceOS-Sml-*
#   VLM Medium models (~1.4GB x4 = 5.6GB) → VLMFiles/{EN,MUL}/VoiceOS-Med-*
#   Total: ~8.1GB
#
# Already tracked in git (no download needed):
#   VLM Tiny models (~74MB x4 = 296MB)    — tracked in git
#   Vivoka SDK AARs/JARs/JNI (~307MB)     — all files <100MB
#   Sherpa-ONNX AAR/JARs (~40MB)          — tracked in git
#   VSDK ASR data (~164MB)                — all files <100MB
#   TVM4J core JARs (~80KB)               — tracked in git
#
# Prerequisites:
#   GITLAB_TOKEN      — Personal Access Token with 'read_api' scope
#                       (or CI_JOB_TOKEN in GitLab CI/CD)
#   GITLAB_PROJECT_ID — Numeric project ID (7692871)
#
# Usage:
#   GITLAB_TOKEN=glpat-XXX GITLAB_PROJECT_ID=7692871 ./scripts/setup-sdk.sh
#   GITLAB_TOKEN=glpat-XXX GITLAB_PROJECT_ID=7692871 ./scripts/setup-sdk.sh --en-only
#   GITLAB_TOKEN=glpat-XXX GITLAB_PROJECT_ID=7692871 ./scripts/setup-sdk.sh --mul-only
#   GITLAB_TOKEN=glpat-XXX GITLAB_PROJECT_ID=7692871 ./scripts/setup-sdk.sh --sml-only
#
set -euo pipefail

# ── Configuration ──────────────────────────────────────────────────────────────

GITLAB_API="${GITLAB_API:-https://gitlab.com/api/v4}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# ── Validation ─────────────────────────────────────────────────────────────────

TOKEN="${GITLAB_TOKEN:-${CI_JOB_TOKEN:-}}"
if [[ -z "$TOKEN" ]]; then
    echo -e "${RED}ERROR: GITLAB_TOKEN or CI_JOB_TOKEN required${NC}"
    echo "  Local dev:  export GITLAB_TOKEN=glpat-XXXXXXXXXXXXXXXXXXXX"
    echo "  GitLab CI:  CI_JOB_TOKEN is auto-available"
    exit 1
fi

if [[ -z "${GITLAB_PROJECT_ID:-}" ]]; then
    echo -e "${RED}ERROR: GITLAB_PROJECT_ID environment variable is required${NC}"
    echo "  NewAvanues project ID: 7692871"
    exit 1
fi

# Select auth header based on token type
if [[ -n "${CI_JOB_TOKEN:-}" && "$TOKEN" == "$CI_JOB_TOKEN" ]]; then
    AUTH_HEADER="Job-Token: ${TOKEN}"
else
    AUTH_HEADER="Private-Token: ${TOKEN}"
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
CHECKSUM_FILE="${SCRIPT_DIR}/checksums.sha256"

cd "$ROOT_DIR"

# ── Parse Arguments ────────────────────────────────────────────────────────────

DOWNLOAD_EN=true
DOWNLOAD_MUL=true
SIZES="all"  # all | sml

for arg in "$@"; do
    case "$arg" in
        --en-only)    DOWNLOAD_MUL=false ;;
        --mul-only)   DOWNLOAD_EN=false ;;
        --sml-only)   SIZES="sml" ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Downloads VLM Base, Small & Medium models (>100MB) from GitLab Package Registry."
            echo "Tiny models (<100MB) are tracked in git — no download needed."
            echo ""
            echo "Options:"
            echo "  --en-only     Only download English models (~4GB)"
            echo "  --mul-only    Only download Multilingual models (~4GB)"
            echo "  --sml-only    Only download Small models (~1.9GB, skip Base & Medium)"
            echo "  --help        Show this help"
            exit 0
            ;;
        *) echo -e "${RED}Unknown argument: $arg${NC}"; exit 1 ;;
    esac
done

# ── Helper Functions ───────────────────────────────────────────────────────────

log_info()  { echo -e "${BLUE}[INFO]${NC} $*"; }
log_ok()    { echo -e "${GREEN}[ OK ]${NC} $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_err()   { echo -e "${RED}[ERR ]${NC} $*"; }
log_skip()  { echo -e "${CYAN}[SKIP]${NC} $*"; }

DOWNLOADED=0
SKIPPED=0
ERRORS=0

# Look up expected SHA-256 from checksums.sha256 for a given relative path.
get_expected_checksum() {
    local path="$1"
    if [[ -f "$CHECKSUM_FILE" ]]; then
        grep -F "  ${path}" "$CHECKSUM_FILE" 2>/dev/null | awk '{print $1}' || true
    fi
}

# Verify a file's SHA-256 matches the expected value.
verify_checksum() {
    local file="$1"
    local expected="$2"
    if [[ -z "$expected" ]]; then
        return 0  # no checksum to verify against
    fi
    local actual
    actual=$(shasum -a 256 "$file" 2>/dev/null | awk '{print $1}')
    if [[ "$actual" == "$expected" ]]; then
        return 0
    else
        log_err "Checksum mismatch: $file"
        log_err "  Expected: $expected"
        log_err "  Actual:   $actual"
        return 1
    fi
}

# Check if a local file already exists and passes checksum.
check_existing() {
    local output="$1"
    if [[ ! -f "$output" ]]; then
        return 1
    fi
    local expected
    expected=$(get_expected_checksum "$output")
    if [[ -n "$expected" ]]; then
        local actual
        actual=$(shasum -a 256 "$output" 2>/dev/null | awk '{print $1}')
        if [[ "$actual" == "$expected" ]]; then
            return 0
        fi
        return 1
    fi
    return 0  # file exists, no checksum → assume OK
}

# Download from GitLab Generic Package Registry.
download_generic() {
    local package_name="$1"
    local version="$2"
    local filename="$3"
    local output="$4"

    if check_existing "$output"; then
        log_skip "$output"
        ((SKIPPED++))
        return 0
    fi

    local url="${GITLAB_API}/projects/${GITLAB_PROJECT_ID}/packages/generic/${package_name}/${version}/${filename}"

    log_info "Downloading ${package_name}/${filename} → ${output}..."
    mkdir -p "$(dirname "$output")"

    local http_code
    http_code=$(curl -s -w "%{http_code}" \
        --header "${AUTH_HEADER}" \
        --output "$output" \
        "$url")

    if [[ "$http_code" != "200" ]]; then
        log_err "HTTP ${http_code} for ${package_name}/${filename}"
        rm -f "$output"
        ((ERRORS++))
        return 1
    fi

    local expected
    expected=$(get_expected_checksum "$output")
    if [[ -n "$expected" ]] && ! verify_checksum "$output" "$expected"; then
        rm -f "$output"
        ((ERRORS++))
        return 1
    fi

    local size
    size=$(du -h "$output" | cut -f1 | xargs)
    log_ok "$output (${size})"
    ((DOWNLOADED++))
}

# ── Main ───────────────────────────────────────────────────────────────────────

echo "═══════════════════════════════════════════════════════════════"
echo "  NewAvanues — Download Large Binaries (>100MB)"
echo "  Project: ${GITLAB_PROJECT_ID}"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Build model list based on size filter
# Tiny (~74MB) is tracked in git — only download Base, Small, and Medium
if [[ "$SIZES" == "sml" ]]; then
    EN_MODELS=("VoiceOS-Sml-EN")
    MUL_MODELS=("VoiceOS-Sml-MUL")
else
    EN_MODELS=("VoiceOS-Bas-EN" "VoiceOS-Sml-EN" "VoiceOS-Med-EN")
    MUL_MODELS=("VoiceOS-Bas-MUL" "VoiceOS-Sml-MUL" "VoiceOS-Med-MUL")
fi

# ── VLM English Models ────────────────────────────────────────────────────────

if [[ "$DOWNLOAD_EN" == true ]]; then
    echo "── VLM Models (English) ─────────────────────────────────────"
    for model in "${EN_MODELS[@]}"; do
        download_generic "vlm-models-en" "1.0.0" "${model}.bin" "VLMFiles/EN/${model}.bin"
        download_generic "vlm-models-en" "1.0.0" "${model}.vlm" "VLMFiles/EN/${model}.vlm"
    done
    echo ""
fi

# ── VLM Multilingual Models ──────────────────────────────────────────────────

if [[ "$DOWNLOAD_MUL" == true ]]; then
    echo "── VLM Models (Multilingual) ──────────────────────────────────"
    for model in "${MUL_MODELS[@]}"; do
        download_generic "vlm-models-mul" "1.0.0" "${model}.bin" "VLMFiles/MUL/${model}.bin"
        download_generic "vlm-models-mul" "1.0.0" "${model}.vlm" "VLMFiles/MUL/${model}.vlm"
    done
    echo ""
fi

# ── Summary ────────────────────────────────────────────────────────────────────

echo "═══════════════════════════════════════════════════════════════"
echo -e "  Downloaded: ${GREEN}${DOWNLOADED}${NC}  Skipped: ${CYAN}${SKIPPED}${NC}  Errors: ${RED}${ERRORS}${NC}"

if [[ $ERRORS -eq 0 ]]; then
    echo -e "  ${GREEN}Download complete!${NC}"
else
    echo -e "  ${RED}Some downloads failed. Re-run to retry.${NC}"
fi
echo "═══════════════════════════════════════════════════════════════"

exit $ERRORS
