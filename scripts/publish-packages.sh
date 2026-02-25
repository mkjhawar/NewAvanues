#!/usr/bin/env bash
#
# publish-packages.sh — Publish binary dependencies to GitLab Package Registry
#
# Uploads Vivoka SDK, Sherpa-ONNX, and VLM model files so that Gradle can
# resolve them as Maven coordinates and new machines can bootstrap via setup-sdk.sh.
#
# Prerequisites:
#   GITLAB_TOKEN      — Personal Access Token with 'api' scope
#   GITLAB_PROJECT_ID — Numeric project ID (Settings → General)
#
# Usage:
#   GITLAB_TOKEN=glpat-XXX GITLAB_PROJECT_ID=12345 ./scripts/publish-packages.sh
#   GITLAB_TOKEN=glpat-XXX GITLAB_PROJECT_ID=12345 ./scripts/publish-packages.sh --sdk-only
#   GITLAB_TOKEN=glpat-XXX GITLAB_PROJECT_ID=12345 ./scripts/publish-packages.sh --vlm-only
#   GITLAB_TOKEN=glpat-XXX GITLAB_PROJECT_ID=12345 ./scripts/publish-packages.sh --asr-only
#   GITLAB_TOKEN=glpat-XXX GITLAB_PROJECT_ID=12345 ./scripts/publish-packages.sh --tvm-only
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
NC='\033[0m'

# ── Validation ─────────────────────────────────────────────────────────────────

if [[ -z "${GITLAB_TOKEN:-}" ]]; then
    echo -e "${RED}ERROR: GITLAB_TOKEN environment variable is required${NC}"
    echo "  Create at: https://gitlab.com/-/user_settings/personal_access_tokens"
    echo "  Required scope: api"
    exit 1
fi

if [[ -z "${GITLAB_PROJECT_ID:-}" ]]; then
    echo -e "${RED}ERROR: GITLAB_PROJECT_ID environment variable is required${NC}"
    echo "  Find at: your-project → Settings → General → Project ID"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "$ROOT_DIR"

# ── Parse Arguments ────────────────────────────────────────────────────────────

PUBLISH_SDK=true
PUBLISH_VLM=true
PUBLISH_ASR=true
PUBLISH_TVM=true

for arg in "$@"; do
    case "$arg" in
        --sdk-only)  PUBLISH_VLM=false; PUBLISH_ASR=false; PUBLISH_TVM=false ;;
        --vlm-only)  PUBLISH_SDK=false; PUBLISH_ASR=false; PUBLISH_TVM=false ;;
        --asr-only)  PUBLISH_SDK=false; PUBLISH_VLM=false; PUBLISH_TVM=false ;;
        --tvm-only)  PUBLISH_SDK=false; PUBLISH_VLM=false; PUBLISH_ASR=false ;;
        --help|-h)
            echo "Usage: $0 [--sdk-only|--vlm-only|--asr-only|--tvm-only]"
            echo "  --sdk-only   Only publish SDK binaries (Vivoka, Sherpa-ONNX)"
            echo "  --vlm-only   Only publish VLM model files"
            echo "  --asr-only   Only publish VSDK ASR data files"
            echo "  --tvm-only   Only publish TVM4J JARs"
            exit 0
            ;;
        *) echo -e "${RED}Unknown argument: $arg${NC}"; exit 1 ;;
    esac
done

# ── Helper Functions ───────────────────────────────────────────────────────────

log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_ok()   { echo -e "${GREEN}[OK]${NC}   $*"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_err()  { echo -e "${RED}[ERR]${NC}  $*"; }

ERRORS=0

# Upload a file to GitLab Maven Package Registry with a minimal POM.
# Usage: publish_maven <file> <artifactId> <version> <packaging>
publish_maven() {
    local file="$1"
    local artifact_id="$2"
    local version="$3"
    local packaging="$4"  # aar or jar
    local filename="${artifact_id}-${version}.${packaging}"
    local url="${GITLAB_API}/projects/${GITLAB_PROJECT_ID}/packages/maven/${GROUP_PATH}/${artifact_id}/${version}/${filename}"

    if [[ ! -f "$file" ]]; then
        log_err "File not found: $file"
        return 1
    fi

    local size
    size=$(du -h "$file" | cut -f1 | xargs)
    log_info "Publishing ${GROUP_ID}:${artifact_id}:${version} (${packaging}, ${size})..."

    # Upload the artifact
    local http_code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" \
        --request PUT \
        --header "Private-Token: ${GITLAB_TOKEN}" \
        --upload-file "$file" \
        "$url")

    if [[ "$http_code" == "200" || "$http_code" == "201" ]]; then
        log_ok "${artifact_id}:${version} → HTTP ${http_code}"
    else
        log_err "${artifact_id}:${version} → HTTP ${http_code}"
        return 1
    fi

    # Generate and upload a minimal POM so Maven/Gradle can resolve metadata
    local pom_file
    pom_file=$(mktemp /tmp/pom-XXXXXX.xml)
    cat > "$pom_file" <<POMEOF
<?xml version="1.0" encoding="UTF-8"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
    xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  <groupId>${GROUP_ID}</groupId>
  <artifactId>${artifact_id}</artifactId>
  <version>${version}</version>
  <packaging>${packaging}</packaging>
</project>
POMEOF

    local pom_url="${GITLAB_API}/projects/${GITLAB_PROJECT_ID}/packages/maven/${GROUP_PATH}/${artifact_id}/${version}/${artifact_id}-${version}.pom"
    http_code=$(curl -s -o /dev/null -w "%{http_code}" \
        --request PUT \
        --header "Private-Token: ${GITLAB_TOKEN}" \
        --upload-file "$pom_file" \
        "$pom_url")
    rm -f "$pom_file"

    if [[ "$http_code" == "200" || "$http_code" == "201" ]]; then
        log_ok "  POM uploaded"
    else
        log_warn "  POM upload → HTTP ${http_code} (non-fatal)"
    fi
}

# Upload a file to GitLab Generic Package Registry.
# Usage: publish_generic <file> <packageName> <version> [overrideFilename]
publish_generic() {
    local file="$1"
    local package_name="$2"
    local version="$3"
    local filename="${4:-$(basename "$file")}"
    local url="${GITLAB_API}/projects/${GITLAB_PROJECT_ID}/packages/generic/${package_name}/${version}/${filename}"

    if [[ ! -f "$file" ]]; then
        log_err "File not found: $file"
        return 1
    fi

    local size
    size=$(du -h "$file" | cut -f1 | xargs)
    log_info "Publishing ${package_name}/${version}/${filename} (${size})..."

    local http_code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" \
        --request PUT \
        --header "Private-Token: ${GITLAB_TOKEN}" \
        --upload-file "$file" \
        "$url")

    if [[ "$http_code" == "200" || "$http_code" == "201" ]]; then
        log_ok "${package_name}/${filename} → HTTP ${http_code}"
    else
        log_err "${package_name}/${filename} → HTTP ${http_code}"
        return 1
    fi
}

# ── Main ───────────────────────────────────────────────────────────────────────

echo "═══════════════════════════════════════════════════════════════"
echo "  GitLab Package Registry Publisher"
echo "  Project: ${GITLAB_PROJECT_ID}"
echo "  Group:   ${GROUP_ID}"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# ── 1. Vivoka SDK AARs → Maven ────────────────────────────────────────────────

if [[ "$PUBLISH_SDK" == true ]]; then
    echo "── Vivoka SDK AARs → Maven ────────────────────────────────"
    publish_maven "vivoka/vsdk-6.0.0.aar"           "vsdk"           "6.0.0" "aar" || ((ERRORS++))
    publish_maven "vivoka/vsdk-csdk-asr-2.0.0.aar"  "vsdk-csdk-asr"  "2.0.0" "aar" || ((ERRORS++))
    publish_maven "vivoka/vsdk-csdk-core-1.0.1.aar" "vsdk-csdk-core" "1.0.1" "aar" || ((ERRORS++))
    echo ""

    # ── 2. Vivoka Android JARs → Maven ────────────────────────────────────────

    echo "── Vivoka Android JARs → Maven ────────────────────────────"
    publish_maven "vivoka/Android/libs/vsdk-6.0.0.jar"           "vsdk-jar"           "6.0.0" "jar" || ((ERRORS++))
    publish_maven "vivoka/Android/libs/vsdk-csdk-asr-2.0.0.jar"  "vsdk-csdk-asr-jar"  "2.0.0" "jar" || ((ERRORS++))
    publish_maven "vivoka/Android/libs/vsdk-csdk-core-1.0.1.jar" "vsdk-csdk-core-jar" "1.0.1" "jar" || ((ERRORS++))
    echo ""

    # ── 3. Vivoka JNI Native Libraries → Generic (tar.gz) ─────────────────────

    echo "── Vivoka JNI Libraries → Generic ─────────────────────────"
    if [[ -d "vivoka/Android/src/main/jniLibs" ]]; then
        JNILIBS_TAR="/tmp/vivoka-jnilibs.tar.gz"
        tar -czf "$JNILIBS_TAR" -C vivoka/Android/src/main jniLibs/
        publish_generic "$JNILIBS_TAR" "vivoka-jnilibs" "6.0.0" "vivoka-jnilibs.tar.gz" || ((ERRORS++))
        rm -f "$JNILIBS_TAR"
    else
        log_warn "vivoka/Android/src/main/jniLibs not found, skipping"
    fi
    echo ""

    # ── 4. Sherpa-ONNX → Maven ─────────────────────────────────────────────────

    echo "── Sherpa-ONNX → Maven ────────────────────────────────────"
    publish_maven "sherpa-onnx/sherpa-onnx.aar"         "sherpa-onnx-android"  "1.5.5" "aar" || ((ERRORS++))
    publish_maven "sherpa-onnx/sherpa-onnx-classes.jar"  "sherpa-onnx-classes"  "1.5.5" "jar" || ((ERRORS++))
    publish_maven "sherpa-onnx/sherpa-onnx-desktop.jar"  "sherpa-onnx-desktop"  "1.5.5" "jar" || ((ERRORS++))
    echo ""
fi

# ── 5. VLM Model Files → Generic ──────────────────────────────────────────────

if [[ "$PUBLISH_VLM" == true ]]; then
    echo "── VLM Models (EN) → Generic ──────────────────────────────"
    for f in VLMFiles/EN/*.bin VLMFiles/EN/*.vlm; do
        [[ -f "$f" ]] && { publish_generic "$f" "vlm-models-en" "1.0.0" || ((ERRORS++)); }
    done
    echo ""

    echo "── VLM Models (MUL) → Generic ─────────────────────────────"
    for f in VLMFiles/MUL/*.bin VLMFiles/MUL/*.vlm; do
        [[ -f "$f" ]] && { publish_generic "$f" "vlm-models-mul" "1.0.0" || ((ERRORS++)); }
    done
    echo ""
fi

# ── 6. VSDK ASR Data → Generic (tar.gz) ──────────────────────────────────────

if [[ "$PUBLISH_ASR" == true ]]; then
    echo "── VSDK ASR Data → Generic ────────────────────────────────"
    ASR_DATA_DIR="Modules/SpeechRecognition/src/main/assets/vsdk/data"
    if [[ -d "$ASR_DATA_DIR" ]]; then
        ASR_TAR="/tmp/vsdk-asr-data-$$.tar.gz"
        tar -czf "$ASR_TAR" -C Modules/SpeechRecognition/src/main/assets/vsdk data/
        publish_generic "$ASR_TAR" "vsdk-asr-data" "1.0.0" "vsdk-asr-data.tar.gz" || ((ERRORS++))
        rm -f "$ASR_TAR"
    else
        log_warn "$ASR_DATA_DIR not found, skipping"
    fi
    echo ""
fi

# ── 7. TVM4J JARs → Maven ───────────────────────────────────────────────────

if [[ "$PUBLISH_TVM" == true ]]; then
    echo "── TVM4J JARs → Maven ───────────────────────────────────"
    publish_maven "Modules/AI/ALC/libs/tvm4j_core.jar"  "tvm4j-core"      "1.0.0" "jar" || ((ERRORS++))
    publish_maven "Modules/AI/LLM/libs/tvm4j_core.jar"  "tvm4j-core-llm"  "1.0.0" "jar" || ((ERRORS++))
    echo ""
fi

# ── Summary ────────────────────────────────────────────────────────────────────

echo "═══════════════════════════════════════════════════════════════"
if [[ $ERRORS -eq 0 ]]; then
    echo -e "  ${GREEN}All packages published successfully!${NC}"
    echo ""
    echo "  Next steps:"
    echo "    1. Run: ./scripts/setup-sdk.sh to verify downloads work"
    echo "    2. Commit and push"
else
    echo -e "  ${RED}${ERRORS} error(s) during publishing${NC}"
    echo "  Check network connectivity and token permissions."
fi
echo "═══════════════════════════════════════════════════════════════"

exit $ERRORS
