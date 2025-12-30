# Model Naming and URL Fixes - Summary Document

**Date:** 2025-11-28
**Version:** 1.0
**Status:** Complete
**Type:** Documentation Update + Code Fixes

---

## Executive Summary

Fixed broken Hugging Face download URLs (404 errors) and enforced **AVA-AON Naming Convention v2** across entire codebase and documentation. All models now use standardized naming, correct download URLs, and proper AON format wrapping.

**Impact:**
- ✅ No more 404 errors when downloading models
- ✅ Consistent naming across all documentation
- ✅ Clear migration path for existing installations
- ✅ Professional model management standards

---

## Problem Statement

### Issue 1: Broken Hugging Face URLs

**URL returning 404:**
```
https://huggingface.co/.../model_quantized.onnx
```

**Root Cause:** File `model_quantized.onnx` does not exist in Hugging Face repositories.

**Correct URL:**
```
https://huggingface.co/.../model_qint8_arm64.onnx
```

### Issue 2: Inconsistent Model Naming

**Multiple naming conventions in use:**
- `all-minilm-l6-v2.onnx` (original Hugging Face names)
- `malbert-multilingual-l12-v2.onnx` (descriptive names)
- `AVA-ONX-384-BASE-INT8.onnx` (intermediate AVA naming)
- `AVA-384-Base-INT8.AON` (correct AVA-AON v2 naming)

**Problem:** Confusion, inconsistency, difficult to manage.

---

## Solution Implemented

### 1. Corrected Download URLs

**all-MiniLM-L6-v2 (English RAG):**
- ❌ OLD: `.../model_quantized.onnx` (404)
- ✅ NEW: `.../model_qint8_arm64.onnx` (23 MB)

**paraphrase-multilingual-MiniLM-L12-v2 (Multilingual NLU):**
- ❌ OLD: `.../model_quantized.onnx` (404)
- ✅ NEW: `.../model_qint8_arm64.onnx` (90 MB)

**Pattern:** Always use `model_qint8_arm64.onnx` for Android ARM devices.

### 2. Enforced AVA-AON Naming Convention v2

**Format:**
```
AVA-{dimension}-{variant}-{quantization}.AON
```

**Examples:**
| Model | Old Name | New Name |
|-------|----------|----------|
| all-MiniLM-L6-v2 | `all-minilm-l6-v2.onnx` | `AVA-384-Base-INT8.AON` |
| paraphrase-multilingual | `malbert-multilingual-l12-v2.onnx` | `AVA-384-Multi-INT8.AON` |
| MobileBERT | `mobilebert-uncased-int8.onnx` | `AVA-384-Mobile-INT8.AON` |

**See:** Developer Manual Chapter 44 for complete specification.

### 3. Updated Storage Locations

**OLD:**
```
/sdcard/Android/data/com.augmentalis.ava/files/models/
```

**NEW:**
```
/sdcard/ava-ai-models/
├── embeddings/    # NLU models
├── rag/           # RAG models
└── llm/           # LLM models
```

**Benefits:**
- Shared across AVA ecosystem apps
- Organized by purpose
- Easier to manage

---

## Files Modified

### Code Files (3)

#### 1. ModelManager.kt
**File:** `Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/ModelManager.kt`

**Changes:**
- Fixed `malbertUrl` from `model_quantized.onnx` → `model_qint8_arm64.onnx`
- Added documentation note about AON wrapping requirement
- Updated size reference from 120 MB → 90 MB (ARM64 quantized)

**Lines:** 61-63

#### 2. ava-testing-guide.md
**File:** `ava-testing-guide.md` (root)

**Changes:** 16 references updated
- Directory structure: Updated to use AVA-AON naming
- mALBERT section: Complete rewrite with 3-step workflow (Download → Wrap → Deploy)
- all-MiniLM section: Updated with correct URL and AON wrapping
- File checklist: All filenames updated to AVA-AON format
- ADB push commands: Updated to new filenames
- Log output examples: Updated paths
- Quick setup script: Updated filenames

**Key Sections:**
- Lines 43-62: Directory structure
- Lines 103-140: mALBERT model setup
- Lines 176-216: all-MiniLM model setup
- Lines 891-898: File checklist
- Lines 439-446: ADB push commands

#### 3. ava-file-locations.md
**File:** `ava-file-locations.md` (root)

**Changes:** 4 references updated
- mALBERT download section: Complete workflow with correct URL
- RAG model deployment: Fixed destination path
- Quick setup script: Updated commands

**Key Sections:**
- Lines 178-209: mALBERT download instructions
- Lines 47-58: RAG model deployment
- Line 337: Script model push

### Documentation Files (2)

#### 4. Developer-Manual-Chapter28B-Model-Naming-Update.md
**File:** `docs/Developer-Manual-Chapter28B-Model-Naming-Update.md`

**Status:** NEW - Created
**Purpose:** Addendum to Chapter 28 with all updated model references

**Contents:**
- Updated naming convention summary
- Corrected model registry table
- Fixed download instructions for all models
- Migration guide for existing installations
- AON format benefits and technical details
- Quick reference table
- Implementation checklist

**Replaces:** Outdated naming in Developer Manual Chapter 28

#### 5. MODEL-NAMING-UPDATE-2025-11-28.md (This Document)
**File:** `docs/MODEL-NAMING-UPDATE-2025-11-28.md`

**Status:** NEW - Created
**Purpose:** Summary of all changes for easy reference

### Local Reference (Not Committed)

#### 6. .claude/ava-model-management-rules.md
**File:** `.claude/ava-model-management-rules.md`

**Status:** NEW - Created (gitignored)
**Purpose:** Project-specific instructions for Claude Code

**Contents:**
- All correct Hugging Face URLs
- AVA-AON naming standards
- Model wrapping workflows
- Common mistakes to avoid
- Verification checklist

**Prevents:** Future recurrence of this issue

---

## Documentation Status

### ✅ Already Compliant (No Changes Needed)

1. **Developer-Manual-Chapter44-AVA-Naming-Convention.md**
   - Already defines correct AVA-AON naming
   - Example: `AVA-384-Base-INT8.AON` (line 91, 159)
   - Status: Reference document, no updates needed

2. **Developer-Manual-Chapter50-External-Storage-Migration.md**
   - Already uses correct AON naming (lines 94-96)
   - Storage paths already updated
   - Status: Fully compliant

3. **User-Manual-Chapter10-Model-Installation.md**
   - User-facing, no broken URLs
   - Uses simplified naming (no `.AON` extension shown to users)
   - Status: No technical URLs to fix

4. **BACKLOG.md**
   - Strategic planning document
   - No specific model filenames or URLs
   - Status: No updates needed

5. **RAG-Phase2-TODO.md**
   - Cross-platform planning document
   - References ONNX and embeddings generically
   - Status: No updates needed

### ⚠️ Needs Manual Review (Not Auto-Fixed)

1. **Developer-Manual-Chapter28-RAG.md**
   - Contains many old references (lines 67, 746-750, 854-910, etc.)
   - **Addressed by:** Chapter28B addendum document
   - **Action:** Review and incorporate addendum into Chapter 28
   - **Priority:** Medium (addendum provides all corrections)

---

## Migration Guide for Users

### For New Installations

Follow the updated instructions in:
- `ava-testing-guide.md` (complete setup)
- `ava-file-locations.md` (file locations)
- `Developer-Manual-Chapter28B-Model-Naming-Update.md` (technical details)

### For Existing Installations

#### Step 1: Download Correct Models
```bash
wget https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model_qint8_arm64.onnx
wget https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model_qint8_arm64.onnx
```

#### Step 2: Wrap in AON Format
```kotlin
import com.augmentalis.ava.features.rag.embeddings.AONFileManager

// English RAG model
AONFileManager.wrapONNX(
    onnxFile = File("model_qint8_arm64.onnx"),
    outputFile = File("AVA-384-Base-INT8.AON"),
    modelId = "AVA-384-Base-INT8",
    allowedPackages = listOf("com.augmentalis.ava")
)

// Multilingual NLU model
AONFileManager.wrapONNX(
    onnxFile = File("model_qint8_arm64.onnx"),
    outputFile = File("AVA-768-Multi-INT8.AON"),
    modelId = "AVA-768-Multi-INT8",
    allowedPackages = listOf("com.augmentalis.ava")
)
```

#### Step 3: Deploy to New Location
```bash
adb shell mkdir -p /sdcard/ava-ai-models/rag
adb shell mkdir -p /sdcard/ava-ai-models/embeddings

adb push AVA-384-Base-INT8.AON /sdcard/ava-ai-models/rag/
adb push AVA-768-Multi-INT8.AON /sdcard/ava-ai-models/embeddings/
adb push vocab.txt /sdcard/ava-ai-models/rag/
adb push vocab.txt /sdcard/ava-ai-models/embeddings/
```

#### Step 4: Remove Old Models (Optional)
```bash
adb shell rm -rf /sdcard/Android/data/com.augmentalis.ava/files/models/*.onnx
```

---

## Verification

### URLs Tested
```bash
# ✅ all-MiniLM-L6-v2
curl -I https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model_qint8_arm64.onnx
# Result: HTTP/2 302 (redirect to CDN, file exists)

# ✅ paraphrase-multilingual-MiniLM-L12-v2
curl -I https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model_qint8_arm64.onnx
# Result: HTTP/2 302 (redirect to CDN, file exists)
```

### Grep Verification
```bash
# No more broken URLs in core files
grep -r "model_quantized\.onnx" --include="*.kt" --include="*.md" \
  --exclude-dir=".git" --exclude-dir=".claude-context-saves"
# Result: Only found in new documentation showing OLD vs NEW comparison
```

---

## Benefits

### 1. Correctness
- ✅ All download URLs now work (no 404 errors)
- ✅ Consistent naming across codebase
- ✅ Clear documentation

### 2. Professional
- ✅ AVA branding on all model files
- ✅ Security features (AON format)
- ✅ Clear versioning and quantization indicators

### 3. Maintainability
- ✅ Easy to identify model purpose from filename
- ✅ Organized storage structure
- ✅ Future-proof naming convention

### 4. Developer Experience
- ✅ Clear setup instructions
- ✅ Correct URLs in documentation
- ✅ Migration path for existing users

---

## Commits

### Commit 1: Code and Testing Docs
**Hash:** 6d5290ea
**Message:** "fix: correct broken Hugging Face URLs and enforce AVA-AON naming conventions"
**Files:** 3
- ModelManager.kt
- ava-testing-guide.md
- ava-file-locations.md

### Commit 2: Developer Manual Updates (This Commit)
**Files:** 2 new
- docs/Developer-Manual-Chapter28B-Model-Naming-Update.md
- docs/MODEL-NAMING-UPDATE-2025-11-28.md

---

## Related Documents

### Primary References
- **Chapter 44:** AVA Naming Convention v2 (authoritative spec)
- **Chapter 28B:** RAG Model Naming Update (this update)
- **Chapter 50:** External Storage Migration (storage paths)

### Setup Guides
- **ava-testing-guide.md:** User setup instructions
- **ava-file-locations.md:** File inventory

### Code References
- **AONFileManager.kt:** Model wrapping implementation
- **ModelManager.kt:** NLU model management
- **ONNXEmbeddingProvider.android.kt:** RAG embedding provider

---

## Action Items

### Completed ✅
- [x] Fix broken URLs in code and documentation
- [x] Update all filenames to AVA-AON convention
- [x] Create Chapter 28B addendum document
- [x] Create summary document
- [x] Create project-specific instructions for Claude Code
- [x] Verify all URLs work
- [x] Commit all changes

### Recommended (Future)
- [ ] Incorporate Chapter 28B updates directly into Chapter 28 (next revision)
- [ ] Create automated script for model download + wrapping
- [ ] Add URL validation tests to CI/CD
- [ ] Create video tutorial for model installation
- [ ] Update User Manual with screenshots of new file structure

---

## Lessons Learned

### Root Causes
1. **Incorrect URL pattern:** Used `model_quantized.onnx` instead of `model_qint8_arm64.onnx`
2. **Naming inconsistency:** Multiple conventions used simultaneously
3. **Incomplete documentation:** AON wrapping workflow not documented

### Prevention
1. **Project-specific rules:** Created `.claude/ava-model-management-rules.md`
2. **Reference documentation:** Chapter 44 as single source of truth
3. **Verification checklist:** Added to all model-related docs
4. **Consistent patterns:** All new docs must follow Chapter 44

---

## Contact

**Questions or Issues:**
- Open issue in project repository
- Reference this document: `MODEL-NAMING-UPDATE-2025-11-28.md`
- Tag: `model-management`, `documentation`

**Author:** AVA AI Team
**Date:** 2025-11-28
**Version:** 1.0
**Status:** Complete

---

**End of Document**
