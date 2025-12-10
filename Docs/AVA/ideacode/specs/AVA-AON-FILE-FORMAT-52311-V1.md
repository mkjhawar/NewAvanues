# AVA-AON File Format Specification v1.0

**Document Version:** 1.0
**Created:** 2025-11-23
**Author:** AVA AI Team
**© Augmentalis Inc, Intelligent Devices LLC**

---

## Executive Summary

AVA-AON (AVA ONNX Naming) is a proprietary file format for distributing ONNX models with built-in security, authentication, and licensing controls. AON files wrap standard ONNX models with a 256-byte authentication header and 128-byte integrity footer, preventing unauthorized use while maintaining compatibility with authorized AVA applications.

**Key Features:**
- **Security**: HMAC-SHA256 signatures, SHA-256 integrity checks
- **Access Control**: Package name whitelist, device fingerprint binding
- **Licensing**: Tier-based access (free/pro/enterprise), expiry timestamps
- **Compatibility**: Standard ONNX inside, backward compatible with legacy .onnx files
- **Protection**: Third-party ONNX loaders fail with "Invalid format" error

---

## File Structure

```
┌─────────────────────────────────────┐
│  AON Header (256 bytes)             │  ← Proprietary authentication
├─────────────────────────────────────┤
│  ONNX Model Data (variable)         │  ← Standard ONNX binary
├─────────────────────────────────────┤
│  AON Footer (128 bytes)             │  ← Integrity verification
└─────────────────────────────────────┘

Total overhead: 384 bytes per model
```

---

## AON Header (256 bytes)

### Structure

| Offset | Size | Field | Description |
|--------|------|-------|-------------|
| 0 | 8 | magic | Magic bytes: `0x41 0x56 0x41 0x2D 0x41 0x4F 0x4E 0x01` ("AVA-AON\x01") |
| 8 | 4 | formatVersion | Format version (1 = v1.0) |
| 12 | 64 | signature | HMAC-SHA256 signature |
| 76 | 32 | modelId | Model identifier (e.g., "AVA-384-Base-INT8") |
| 108 | 4 | modelVersion | Model version number |
| 112 | 8 | createdTimestamp | Unix timestamp (seconds) when file was created |
| 120 | 8 | expiryTimestamp | Unix timestamp (0 = no expiry) |
| 128 | 1 | licenseTier | 0=free, 1=pro, 2=enterprise |
| 129 | 15 | reserved1 | Reserved for future use |
| 144 | 1 | encryptionScheme | 0=none, 1=AES-256-GCM, 2=ChaCha20-Poly1305 |
| 145 | 16 | ivNonce | Initialization vector (if encrypted) |
| 161 | 15 | reserved2 | Reserved for future use |
| 176 | 8 | onnxDataOffset | Byte offset to ONNX data (typically 256) |
| 184 | 8 | onnxDataSize | Size of ONNX model in bytes |
| 192 | 16 | onnxSHA256 | First 16 bytes of ONNX SHA-256 hash |
| 208 | 48 | allowedPackages | Up to 3 package name MD5 hashes (3 × 16 bytes) |
| 256 | — | — | End of header |

### Magic Bytes Breakdown

```
Hex:   0x41  0x56  0x41  0x2D  0x41  0x4F  0x4E  0x01
ASCII:  'A'   'V'   'A'   '-'   'A'   'O'   'N'   '\x01'
```

**Why this breaks third-party ONNX loaders:**
- ONNX files start with Protocol Buffer magic: `0x08 0x03 0x12 ...`
- AON files start with `0x41` (ASCII 'A')
- ONNX Runtime immediately rejects: `"Invalid ONNX file: expected protobuf magic 0x08"`

---

## AON Footer (128 bytes)

### Structure

| Offset | Size | Field | Description |
|--------|------|-------|-------------|
| 0 | 32 | headerHash | SHA-256 of header |
| 32 | 32 | onnxHash | SHA-256 of ONNX data |
| 64 | 8 | footerMagic | `0x45 0x4E 0x44 0x41 0x4F 0x4E 0x01 0x00` ("ENDAON\x01\x00") |
| 72 | 8 | fileSize | Total file size (header + onnx + footer) |
| 80 | 4 | checksumCRC32 | CRC32 of entire file |
| 84 | 12 | reserved4 | Reserved for future use |
| 96 | 4 | buildNumber | AVA app build that created this file |
| 100 | 16 | creatorSignature | "Augmentalis Inc" (padded) |
| 116 | 12 | reserved5 | Reserved for future use |
| 128 | — | — | End of footer |

---

## Security Features

### 1. HMAC-SHA256 Signature

**Purpose:** Verify file authenticity and prevent tampering

**Computation:**
```kotlin
HMAC-SHA256(
    key = MASTER_SECRET_KEY,
    data = header_bytes + SHA256(onnx_data)
)
```

**Master Key:**
- Embedded in app binary (obfuscated via ProGuard/R8)
- Rotated per app version
- Stored in BuildConfig (not in source code)

### 2. Package Name Whitelist

**Purpose:** Restrict which apps can load the model

**Allowed Packages (default):**
- `com.augmentalis.ava` (AVA Standalone)
- `com.augmentalis.avaconnect` (AVA Connect)
- `com.augmentalis.avanue` (Avanue Platform)

**Storage:** MD5 hash of package name (16 bytes each)

**Verification:**
```kotlin
val packageHash = MD5(context.packageName)
val allowed = header.allowedPackages.any { it == packageHash }
if (!allowed) throw SecurityException("Unauthorized package")
```

### 3. Expiry Timestamp

**Purpose:** Time-limited model access (e.g., trial licenses)

**Format:** Unix timestamp (seconds since epoch)

**Special Values:**
- `0` = No expiry (permanent)
- `> 0` = Expires at timestamp

**Verification:**
```kotlin
if (expiryTimestamp > 0 && now() > expiryTimestamp) {
    throw SecurityException("Model has expired")
}
```

### 4. License Tier

**Purpose:** Differentiate model access levels

| Tier | Value | Description |
|------|-------|-------------|
| Free | 0 | Basic models (384-dim, English) |
| Pro | 1 | Premium models (768-dim, multilingual) |
| Enterprise | 2 | Custom models, unlimited |

### 5. SHA-256 Integrity Checks

**Three-level verification:**
1. **Header Integrity**: `SHA256(header) == footer.headerHash`
2. **ONNX Integrity**: `SHA256(onnx_data) == footer.onnxHash`
3. **ONNX Match**: `SHA256(onnx_data)[0:16] == header.onnxSHA256`

### 6. Optional Encryption (Future)

**Supported Schemes:**
- `0`: No encryption (default for v1.0)
- `1`: AES-256-GCM (Phase 2)
- `2`: ChaCha20-Poly1305 (Phase 3)

**Encrypted Payload:**
```
onnxDataEncrypted = AES-256-GCM(
    key = deriveKeyFromMaster(header.modelId),
    iv = header.ivNonce,
    data = onnxData
)
```

---

## Unwrapping Process

### Step-by-Step Verification

```kotlin
fun unwrapAON(file: File, context: Context): ByteArray {
    // 1. Read header (256 bytes)
    val header = file.read(0, 256)

    // 2. Verify magic bytes
    if (header.magic != "AVA-AON\x01") {
        throw SecurityException("Invalid AON file")
    }

    // 3. Check format version
    if (header.formatVersion != 1) {
        throw IllegalStateException("Unsupported format version")
    }

    // 4. Check expiry
    if (header.expiryTimestamp > 0 && now() > header.expiryTimestamp) {
        throw SecurityException("Model expired")
    }

    // 5. Verify package authorization
    val packageHash = MD5(context.packageName)
    if (packageHash !in header.allowedPackages) {
        throw SecurityException("Unauthorized package")
    }

    // 6. Read ONNX data
    val onnxData = file.read(header.onnxDataOffset, header.onnxDataSize)

    // 7. Verify ONNX hash
    if (SHA256(onnxData)[0:16] != header.onnxSHA256) {
        throw SecurityException("ONNX data corrupted")
    }

    // 8. Read footer (128 bytes)
    val footer = file.read(header.onnxDataOffset + header.onnxDataSize, 128)

    // 9. Verify footer magic
    if (footer.footerMagic != "ENDAON\x01\x00") {
        throw SecurityException("Invalid footer")
    }

    // 10. Verify header integrity
    if (SHA256(header) != footer.headerHash) {
        throw SecurityException("Header tampered")
    }

    // 11. Verify ONNX integrity
    if (SHA256(onnxData) != footer.onnxHash) {
        throw SecurityException("ONNX data tampered")
    }

    // 12. Verify HMAC signature
    val expectedSignature = HMAC_SHA256(MASTER_KEY, header + SHA256(onnxData))
    if (header.signature != expectedSignature) {
        throw SecurityException("Signature verification failed")
    }

    // 13. Decrypt if encrypted
    val finalData = if (header.encryptionScheme != 0) {
        decrypt(onnxData, header)
    } else {
        onnxData
    }

    return finalData
}
```

---

## Usage

### Wrapping ONNX Files

**Command-line tool:**
```bash
./gradlew :Universal:AVA:Features:RAG:runAONWrapper \
  --args="wrap \
  --input=/path/to/all-MiniLM-L6-v2.onnx \
  --output=/path/to/AVA-384-Base-INT8.aon \
  --model-id=AVA-384-Base-INT8 \
  --version=1 \
  --license=0 \
  --expiry-days=365"
```

**Programmatic:**
```kotlin
AONFileManager.wrapONNX(
    onnxFile = File("all-MiniLM-L6-v2.onnx"),
    outputFile = File("AVA-384-Base-INT8.aon"),
    modelId = "AVA-384-Base-INT8",
    modelVersion = 1,
    allowedPackages = listOf(
        "com.augmentalis.ava",
        "com.augmentalis.avaconnect"
    ),
    expiryTimestamp = System.currentTimeMillis() / 1000 + (365 * 86400),
    licenseTier = 0
)
```

### Unwrapping in App

**Automatic (integrated in ONNXEmbeddingProvider):**
```kotlin
val provider = ONNXEmbeddingProvider(
    context = context,
    modelId = "AVA-384-Base-INT8"
)
// Automatically unwraps AON files, uses ONNX files directly
val embedding = provider.embed("Hello world")
```

**Manual:**
```kotlin
val onnxData = AONFileManager.unwrapAON(
    aonFile = File("/sdcard/ava-ai-models/embeddings/AVA-384-Base-INT8.aon"),
    context = context
)
// onnxData is ready for ONNX Runtime
```

---

## Backward Compatibility

### Legacy .onnx Files

**AONFileManager automatically detects file type:**
```kotlin
fun unwrapIfNeeded(file: File): File {
    if (AONFileManager.isAONFile(file)) {
        // Unwrap AON
        return unwrapAON(file)
    } else {
        // Use ONNX directly
        return file
    }
}
```

**File type detection:**
```kotlin
fun isAONFile(file: File): Boolean {
    val magic = file.read(0, 8)
    return magic == "AVA-AON\x01"
}
```

---

## Security Best Practices

### 1. Master Key Management

**Development:**
```kotlin
// In BuildConfig (not source)
const val MASTER_KEY = "AVA-AON-HMAC-SECRET-KEY-V1-CHANGE-IN-PRODUCTION"
```

**Production:**
- Generate unique key per app version
- Store in environment variable during build
- Obfuscate via ProGuard/R8 string encryption
- Rotate quarterly

### 2. Package Name Verification

**Always verify package:**
```kotlin
val currentPackage = context.packageName
val allowed = header.allowedPackages.any {
    MD5(currentPackage) == it
}
```

**Whitelist management:**
- Limit to 3 packages maximum
- Use MD5 hashes (not plaintext)
- Update via server-side config (future)

### 3. Integrity Checks

**Always verify all hashes:**
1. Header hash (footer.headerHash)
2. ONNX hash (footer.onnxHash)
3. ONNX partial hash (header.onnxSHA256)
4. HMAC signature (header.signature)

**Fail securely:**
```kotlin
if (anyCheckFails) {
    throw SecurityException("Verification failed")
    // DO NOT return partial data
}
```

### 4. Expiry Handling

**Check on every load:**
```kotlin
if (expiryTimestamp > 0 && now() > expiryTimestamp) {
    throw SecurityException("Model expired")
    // DO NOT cache expired models
}
```

---

## Error Handling

### Common Errors

| Error | Cause | Solution |
|-------|-------|----------|
| `Invalid AON file: bad magic bytes` | Not an AON file | Use correct file |
| `Package not authorized` | Wrong app package | Add to whitelist |
| `AON file has expired` | Past expiry timestamp | Renew license |
| `ONNX data integrity check failed` | File corrupted | Re-download |
| `HMAC signature verification failed` | File tampered | Re-download from trusted source |
| `Unsupported AON format version` | Future version | Update app |

### Security Exceptions

**Never suppress SecurityException:**
```kotlin
try {
    unwrapAON(file, context)
} catch (e: SecurityException) {
    // DO NOT ignore - log and fail
    log.error("Security violation: ${e.message}")
    throw e
}
```

---

## Future Enhancements (Roadmap)

### Phase 2 (v1.1)
- AES-256-GCM encryption support
- Device fingerprint binding
- Server-side license verification

### Phase 3 (v1.2)
- Online license activation
- Usage analytics (model load count)
- Dynamic package whitelist updates

### Phase 4 (v2.0)
- Model streaming (progressive download)
- Delta updates (model patches)
- Multi-tier quantization (int4/int8/fp16)

---

## Appendix: Example Workflow

### 1. Create AON File

```bash
# Download original ONNX model
wget https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx

# Wrap with AON
./gradlew :Universal:AVA:Features:RAG:runAONWrapper \
  --args="wrap \
  --input=model.onnx \
  --output=AVA-384-Base-INT8.aon \
  --model-id=AVA-384-Base-INT8 \
  --version=1 \
  --license=0"

# Verify
./gradlew :Universal:AVA:Features:RAG:runAONWrapper \
  --args="verify --file=AVA-384-Base-INT8.aon"
```

### 2. Deploy to Device

```bash
# Copy to unified model repository
adb push AVA-384-Base-INT8.aon /sdcard/ava-ai-models/embeddings/

# Or app-specific location
adb push AVA-384-Base-INT8.aon /sdcard/Android/data/com.augmentalis.ava/files/models/
```

### 3. Use in App

```kotlin
// RAG setup
val ragConfig = RAGConfig(
    embeddingConfig = EmbeddingConfig(
        preferredProvider = EmbeddingProvider.ONNX,
        modelName = "AVA-384-Base-INT8",  // AON file auto-detected
        dimension = 384,
        quantize = true
    )
)

// Model automatically unwrapped on first use
val ragEngine = RAGChatEngine(config = ragConfig)
```

### 4. Monitor Usage

```kotlin
// Check unwrap logs
Log.d("AONFileManager", "Unwrapping AON file: AVA-384-Base-INT8.aon")
Log.d("AONFileManager", "Package: com.augmentalis.ava [AUTHORIZED]")
Log.d("AONFileManager", "License: Tier 0 (Free)")
Log.d("AONFileManager", "Expiry: Never")
Log.d("AONFileManager", "Verification: PASSED (SHA256, HMAC)")
```

---

**Document Status:** Final v1.0
**Last Updated:** 2025-11-23
**Maintained By:** AVA AI Team
