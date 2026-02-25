#!/usr/bin/env bash
#
# setup-sdk.sh — Bootstrap binary dependencies for NewAvanues
#
# Downloads large binary files from GitLab Package Registry that are not
# tracked in git. Run this once after cloning, or after a clean checkout.
#
# What gets downloaded:
#   --sdk-only : Vivoka AARs/JARs/JNI + Sherpa-ONNX AAR/JARs (~310MB)
#   --vlm-only : VLM Whisper model files (~8.4GB)
#   (default)  : Everything
#
# Prerequisites:
#   GITLAB_TOKEN      — Personal Access Token with 'read_api' scope
#                       (or CI_JOB_TOKEN in GitLab CI/CD)
#   GITLAB_PROJECT_ID — Numeric project ID
#
# Usage:
#   GITLAB_TOKEN=glpat-XXX GITLAB_PROJECT_ID=12345 ./scripts/setup-sdk.sh
#   GITLAB_TOKEN=glpat-XXX GITLAB_PROJECT_ID=12345 ./scripts/setup-sdk.sh --vlm-only
#   GITLAB_TOKEN=glpat-XXX GITLAB_PROJECT_ID=12345 ./scripts/setup-sdk.sh --sdk-only
#
set -euo pipefail

# ── Configuration ──────────────────────────────────────────────────────────────

GITLAB_API="${GITLAB_API:-https://gitlab.com/api/v4}"
GROUP_ID="com.augmentalis.sdk"
GROUP_PATH="com/augmentalis/sdk"

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
    echo "  Find at: your-project → Settings → General → Project ID"
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

DOWNLOAD_SDK=true
DOWNLOAD_VLM=true

for arg in "$@"; do
    case "$arg" in
        --vlm-only)  DOWNLOAD_SDK=false ;;
        --sdk-only)  DOWNLOAD_VLM=false ;;
        --help|-h)
            echo "Usage: $0 [--vlm-only|--sdk-only]"
            echo "  --vlm-only   Only download VLM model files (~8.4GB)"
            echo "  --sdk-only   Only download SDK binaries (~310MB)"
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
# Returns 0 if file is present and valid (skip download), 1 otherwise.
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
            return 0  # file exists, checksum matches
        fi
        return 1  # file exists but checksum mismatch → re-download
    fi
    return 0  # file exists, no checksum to check → assume OK
}

# Download from GitLab Maven Package Registry.
# Usage: download_maven <artifactId> <version> <packaging> <output_path>
download_maven() {
    local artifact_id="$1"
    local version="$2"
    local packaging="$3"
    local output="$4"

    if check_existing "$output"; then
        log_skip "$output"
        ((SKIPPED++))
        return 0
    fi

    local filename="${artifact_id}-${version}.${packaging}"
    local url="${GITLAB_API}/projects/${GITLAB_PROJECT_ID}/packages/maven/${GROUP_PATH}/${artifact_id}/${version}/${filename}"

    log_info "Downloading ${GROUP_ID}:${artifact_id}:${version} → ${output}..."
    mkdir -p "$(dirname "$output")"

    local http_code
    http_code=$(curl -s -w "%{http_code}" \
        --header "${AUTH_HEADER}" \
        --output "$output" \
        "$url")

    if [[ "$http_code" != "200" ]]; then
        log_err "HTTP ${http_code} for ${artifact_id}:${version}"
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

# Download from GitLab Generic Package Registry.
# Usage: download_generic <packageName> <version> <filename> <output_path>
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
echo "  NewAvanues SDK Bootstrap"
echo "  Project: ${GITLAB_PROJECT_ID}"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# ── SDK Binaries ───────────────────────────────────────────────────────────────

if [[ "$DOWNLOAD_SDK" == true ]]; then

    echo "── Vivoka SDK AARs ──────────────────────────────────────────"
    download_maven "vsdk"           "6.0.0" "aar" "vivoka/vsdk-6.0.0.aar"
    download_maven "vsdk-csdk-asr"  "2.0.0" "aar" "vivoka/vsdk-csdk-asr-2.0.0.aar"
    download_maven "vsdk-csdk-core" "1.0.1" "aar" "vivoka/vsdk-csdk-core-1.0.1.aar"
    echo ""

    echo "── Vivoka Android JARs ──────────────────────────────────────"
    download_maven "vsdk-jar"           "6.0.0" "jar" "vivoka/Android/libs/vsdk-6.0.0.jar"
    download_maven "vsdk-csdk-asr-jar"  "2.0.0" "jar" "vivoka/Android/libs/vsdk-csdk-asr-2.0.0.jar"
    download_maven "vsdk-csdk-core-jar" "1.0.1" "jar" "vivoka/Android/libs/vsdk-csdk-core-1.0.1.jar"
    echo ""

    echo "── Vivoka JNI Native Libraries ──────────────────────────────"
    if [[ ! -d "vivoka/Android/src/main/jniLibs/arm64-v8a" ]]; then
        log_info "Downloading vivoka JNI native libraries..."
        local_tar="/tmp/vivoka-jnilibs-$$.tar.gz"
        url="${GITLAB_API}/projects/${GITLAB_PROJECT_ID}/packages/generic/vivoka-jnilibs/6.0.0/vivoka-jnilibs.tar.gz"

        http_code=$(curl -s -w "%{http_code}" \
            --header "${AUTH_HEADER}" \
            --output "$local_tar" \
            "$url")

        if [[ "$http_code" == "200" ]]; then
            mkdir -p "vivoka/Android/src/main"
            tar -xzf "$local_tar" -C vivoka/Android/src/main
            rm -f "$local_tar"
            log_ok "vivoka/Android/src/main/jniLibs/ extracted"
            ((DOWNLOADED++))
        else
            log_err "JNI libs download failed → HTTP ${http_code}"
            rm -f "$local_tar"
            ((ERRORS++))
        fi
    else
        log_skip "vivoka/Android/src/main/jniLibs/"
        ((SKIPPED++))
    fi
    echo ""

    echo "── Sherpa-ONNX ──────────────────────────────────────────────"
    download_maven "sherpa-onnx-android"  "1.5.5" "aar" "sherpa-onnx/sherpa-onnx.aar"
    download_maven "sherpa-onnx-classes"  "1.5.5" "jar" "sherpa-onnx/sherpa-onnx-classes.jar"
    download_maven "sherpa-onnx-desktop"  "1.5.5" "jar" "sherpa-onnx/sherpa-onnx-desktop.jar"
    echo ""
fi

# ── VLM Model Files ───────────────────────────────────────────────────────────

if [[ "$DOWNLOAD_VLM" == true ]]; then

    echo "── VLM Models (EN) ──────────────────────────────────────────"
    for model in VoiceOS-Tin-EN VoiceOS-Bas-EN VoiceOS-Sml-EN VoiceOS-Med-EN; do
        download_generic "vlm-models-en" "1.0.0" "${model}.bin" "VLMFiles/EN/${model}.bin"
        download_generic "vlm-models-en" "1.0.0" "${model}.vlm" "VLMFiles/EN/${model}.vlm"
    done
    echo ""

    echo "── VLM Models (MUL) ─────────────────────────────────────────"
    for model in VoiceOS-Tin-MUL VoiceOS-Bas-MUL VoiceOS-Sml-MUL VoiceOS-Med-MUL; do
        download_generic "vlm-models-mul" "1.0.0" "${model}.bin" "VLMFiles/MUL/${model}.bin"
        download_generic "vlm-models-mul" "1.0.0" "${model}.vlm" "VLMFiles/MUL/${model}.vlm"
    done
    echo ""
fi

# ── Summary ────────────────────────────────────────────────────────────────────

echo "═══════════════════════════════════════════════════════════════"
echo -e "  Downloaded: ${GREEN}${DOWNLOADED}${NC}  Skipped: ${CYAN}${SKIPPED}${NC}  Errors: ${RED}${ERRORS}${NC}"

if [[ $ERRORS -eq 0 ]]; then
    echo -e "  ${GREEN}Bootstrap complete!${NC}"
    if [[ "$DOWNLOAD_SDK" == true ]]; then
        echo ""
        echo "  Verify build:  ./gradlew :Modules:SpeechRecognition:compileKotlinAndroid"
    fi
else
    echo -e "  ${RED}Some downloads failed. Re-run to retry.${NC}"
fi
echo "═══════════════════════════════════════════════════════════════"

exit $ERRORS
