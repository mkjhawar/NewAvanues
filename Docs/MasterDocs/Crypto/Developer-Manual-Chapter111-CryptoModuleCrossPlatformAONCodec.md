# Chapter 111: Crypto Module — Cross-Platform AON Codec

> **Module**: `Modules/Crypto/`
> **Package**: `com.augmentalis.crypto`
> **Targets**: Android, iOS, macOS, Desktop (JVM), JS (Browser + Node.js)
> **Dependencies**: `kotlinx-coroutines-core` only
> **Consumers**: `Modules/AI/NLU/`, `Modules/AI/RAG/` (future)

---

## 1. Overview

The Crypto module provides unified model file security across all AVA platforms. It implements the AON (AVA ONNX Naming) file format — a binary wrapper around ONNX model files that adds HMAC-SHA256 authentication, SHA-256/CRC32 integrity checking, package/identity whitelisting, and optional AES-256-GCM encryption.

**Before this module**: Only Android (via `Modules/AI/RAG/`) had AON verification. All other platforms either loaded raw ONNX files or did a naive header strip.

**After this module**: Every platform that loads an AVA model file gets the same 8-step verification pipeline.

---

## 2. Architecture

### Source Set Hierarchy

```
commonMain/                          ← expect declarations + AONFormat constants
├── aon/AONFormat.kt                 ← Header/footer layout, magic bytes, byte helpers
├── aon/AONHeader.kt                 ← Parsed header data class
├── aon/AONFooter.kt                 ← Parsed footer data class
├── aon/AONCodec.kt                  ← expect: wrap/unwrap/verify/isAON
├── digest/CryptoDigest.kt           ← expect: SHA-256, MD5, HMAC-SHA256, CRC32
└── identity/PlatformIdentity.kt     ← expect: app identifier + type

jvmMain/                             ← Shared by Android + Desktop
├── aon/AONCodec.kt                  ← actual: javax.crypto verification pipeline
└── digest/CryptoDigest.kt           ← actual: MessageDigest, Mac, java.util.zip.CRC32

androidMain/                         ← Android-only
└── identity/PlatformIdentity.kt     ← actual: context.packageName via reflection

desktopMain/                         ← Desktop JVM only
└── identity/PlatformIdentity.kt     ← actual: system property / process name

darwinMain/                          ← Shared by iOS + macOS
├── aon/AONCodec.kt                  ← actual: CommonCrypto verification pipeline
├── digest/CryptoDigest.kt           ← actual: CC_SHA256, CCHmac, CC_MD5
└── identity/PlatformIdentity.kt     ← actual: Bundle.main.bundleIdentifier

jsMain/                              ← Browser + Node.js
├── aon/AONCodec.kt                  ← actual: crypto.subtle / Node crypto
├── digest/CryptoDigest.kt           ← actual: SubtleCrypto + Node crypto runtime detection
└── identity/PlatformIdentity.kt     ← actual: window.origin / process.env
```

### Key Design Decisions

1. **`jvmMain` shares AONCodec + CryptoDigest** between Android and Desktop — the verification logic and crypto primitives are identical on javax.crypto.

2. **PlatformIdentity is NOT in jvmMain** — it differs per platform (Android uses package name via reflection, Desktop uses system property), so it lives in `androidMain` and `desktopMain` separately.

3. **`darwinMain` shares all three** (AONCodec + CryptoDigest + PlatformIdentity) between iOS and macOS — CommonCrypto is available on both, and both use `Bundle.main.bundleIdentifier`.

4. **JS uses runtime detection** — `isNodeJs` flag checks `typeof process !== 'undefined'` to switch between `crypto.subtle` (browser) and `require('crypto')` (Node.js).

---

## 3. AON File Format

### Header (256 bytes, Little-Endian)

| Offset | Size | Field | Description |
|--------|------|-------|-------------|
| 0 | 8 | magic | `AVA-AON\x01` |
| 8 | 4 | formatVersion | Currently 1 |
| 12 | 64 | signature | HMAC-SHA256 (doubled to 64 bytes) |
| 76 | 32 | modelId | Null-padded ASCII |
| 108 | 4 | modelVersion | Integer version |
| 112 | 8 | createdTimestamp | Unix seconds |
| 120 | 8 | expiryTimestamp | 0 = no expiry |
| 128 | 1 | licenseTier | 0=free, 1=pro, 2=enterprise |
| 129 | 1 | platformFlags | b0=Android b1=iOS b2=Desktop b3=Web b4=Node |
| 144 | 1 | encryptionScheme | 0=none, 1=AES-256-GCM |
| 145 | 16 | ivNonce | AES-GCM IV (12 bytes used) |
| 176 | 8 | onnxDataOffset | Always 256 |
| 184 | 8 | onnxDataSize | Payload byte count |
| 192 | 16 | onnxSHA256Trunc | First 16 bytes of SHA-256(ONNX) |
| 208 | 48 | allowedPackages | 3 x 16-byte MD5 hashes |

### Footer (128 bytes, Little-Endian)

| Offset | Size | Field | Description |
|--------|------|-------|-------------|
| 0 | 32 | headerHash | SHA-256 of entire header |
| 32 | 32 | onnxHash | SHA-256 of ONNX payload |
| 64 | 8 | footerMagic | `ENDAON\x01\x00` |
| 72 | 8 | fileSize | Total file size |
| 80 | 4 | checksumCRC32 | CRC32(header + ONNX) |
| 96 | 4 | buildNumber | Build number |
| 100 | 16 | creatorSignature | Null-padded ASCII |

---

## 4. Verification Pipeline

Every `AONCodec.unwrap()` call performs these 8 checks in order:

1. **Magic bytes** — first 8 bytes == `AVA-AON\x01`
2. **Format version** — must be supported version (currently 1)
3. **HMAC-SHA256** — zeros signature field in header copy, computes HMAC over header + SHA-256(ONNX), compares to stored 64-byte signature (first 32 bytes doubled)
4. **SHA-256 truncated** — hashes ONNX payload, compares first 16 bytes to header field
5. **Footer SHA-256** — compares full 32-byte hash to footer field
6. **CRC32** — computes CRC32(header + ONNX), compares to footer
7. **Expiry** — if expiryTimestamp > 0, checks against current time
8. **Identity** — if any allowedPackage slot is non-zero, MD5-hashes caller identity and matches

If any check fails, `unwrap()` throws `IllegalStateException` with details. For non-AON data (no magic bytes), it returns the data as-is for backward compatibility.

### HMAC Key

The signing key is stored as XOR-masked fragments in `AONFormat.getDefaultHmacKey()` to prevent trivial string extraction from compiled binaries. The same key is used across all platforms.

**Platform-specific overrides**:
| Platform | Override Source |
|----------|---------------|
| Android | `System.getProperty("aon.hmac.key")` |
| Desktop | `System.getProperty("aon.hmac.key")` |
| iOS/macOS | Compiled constant only (Keychain in future) |
| Node.js | `process.env.AON_HMAC_KEY` |
| Browser | Compiled constant (crypto.subtle non-extractable import) |

---

## 5. CryptoDigest API

```kotlin
expect object CryptoDigest {
    suspend fun sha256(data: ByteArray): ByteArray           // 32 bytes
    suspend fun sha256Truncated16(data: ByteArray): ByteArray // 16 bytes
    suspend fun md5(data: ByteArray): ByteArray               // 16 bytes
    suspend fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray // 32 bytes
    fun crc32(vararg chunks: ByteArray): Int                  // CRC32 of concatenated chunks
}
```

**Why `suspend`**: Browser `crypto.subtle` is Promise-based. JVM/Darwin implementations are synchronous but use the same suspend signature for uniformity.

**CRC32**: Pure Kotlin implementation in `commonMain` — uses the standard IEEE polynomial lookup table. Identical across all platforms.

---

## 6. PlatformIdentity API

```kotlin
expect object PlatformIdentity {
    fun getAppIdentifier(): String  // Package name, bundle ID, origin, etc.
    fun getIdentityType(): String   // "android_package", "ios_bundle", "js_origin", etc.
}
```

| Platform | Identity | Type String |
|----------|----------|-------------|
| Android | `context.packageName` (via reflection) | `"android_package"` |
| Desktop | `System.getProperty("aon.app.id")` or process name | `"jvm_system_property"` |
| iOS/macOS | `Bundle.main.bundleIdentifier` | `"ios_bundle"` / `"macos_bundle"` |
| Browser | `window.location.origin` | `"js_origin"` |
| Node.js | `process.env.AON_APP_ID` or `process.title` | `"nodejs_env"` |

---

## 7. Consumer Integration

### NLU ModelManager

All three platform ModelManagers (Android, Desktop, JS) now call `AONCodec.unwrap()`:

```kotlin
// Desktop/Android — new getUnwrappedModelBytes() method
suspend fun getUnwrappedModelBytes(): ByteArray = withContext(Dispatchers.IO) {
    val rawBytes = modelFile.readBytes()
    AONCodec.unwrap(rawBytes)  // Full HMAC + integrity verification
}

// JS — inline in downloadModelsIfNeeded()
val rawBytes = arrayBufferToByteArray(rawModelBuffer)
val onnxBytes = AONCodec.unwrap(rawBytes)
val modelBuffer = byteArrayToArrayBuffer(onnxBytes)
```

**Backward compatibility**: If a model file doesn't start with AON magic bytes, `unwrap()` returns the data unchanged. This means the same code path handles both `.AON` wrapped files and legacy raw `.onnx` files.

### Adding Crypto to a module

```kotlin
// In your module's build.gradle.kts
sourceSets {
    commonMain.dependencies {
        implementation(project(":Modules:Crypto"))
    }
}
```

---

## 8. Testing

23 tests in `commonTest` run on all platforms:

| Category | Tests | Coverage |
|----------|-------|----------|
| Magic byte detection | 5 | Valid, invalid, too small, empty, raw ONNX |
| Header parsing | 4 | Model ID, timestamps, license tier, packages |
| Footer parsing | 2 | Valid magic, invalid magic |
| Byte helpers | 3 | Int LE, Long LE, string extract |
| CryptoDigest | 5 | CRC32 (empty, known, multi-chunk), SHA-256, MD5, HMAC |
| AONCodec passthrough | 1 | Non-AON data returns as-is |
| HMAC key deobfuscation | 1 | XOR mask produces correct key |

Run tests:
```bash
./gradlew :Modules:Crypto:desktopTest      # JVM tests
./gradlew :Modules:Crypto:jsNodeTest        # JS/Node.js tests
./gradlew :Modules:Crypto:iosSimulatorArm64Test  # iOS tests
```

---

## 9. Security Notes

### Browser-Specific Hardening

- **crypto.subtle non-extractable keys**: HMAC key imported with `extractable: false` — once imported, JavaScript cannot read the key material back. This is actually stronger than JVM where keys in memory can be dumped.

- **Pure Kotlin MD5**: Browser fallback since `crypto.subtle` doesn't support MD5 (only used for package hash comparison, not security-critical).

- **Pure Kotlin SHA-256**: Used for synchronous AES key derivation in browser context where `crypto.subtle` would require async.

### Defense in Depth

The XOR-obfuscated key is the minimum viable protection — it prevents `strings` extraction from compiled binaries. Future hardening (Phase 4 of the plan) includes:

- WASM-embedded key for JS (compiled binary, harder to extract)
- RASP (Runtime Application Self-Protection) for tamper detection
- Domain locking for browser (origin whitelist)

---

## 10. File Inventory

| File | Lines | Role |
|------|-------|------|
| `build.gradle.kts` | 152 | Module config, 6 targets, cinterop |
| `commonMain/.../AONFormat.kt` | 211 | Constants, offsets, byte helpers, HMAC key |
| `commonMain/.../AONHeader.kt` | 95 | Header data class + parse |
| `commonMain/.../AONFooter.kt` | 74 | Footer data class + parse |
| `commonMain/.../AONCodec.kt` | 75 | expect: unwrap, verify, isAON, parseHeader |
| `commonMain/.../CryptoDigest.kt` | 37 | expect: SHA-256, MD5, HMAC, CRC32 |
| `commonMain/.../PlatformIdentity.kt` | 35 | expect: identity + type |
| `jvmMain/.../AONCodec.kt` | 226 | actual: JVM verification pipeline |
| `jvmMain/.../CryptoDigest.kt` | 53 | actual: javax.crypto |
| `androidMain/.../PlatformIdentity.kt` | 41 | actual: reflection packageName |
| `desktopMain/.../PlatformIdentity.kt` | 34 | actual: system property |
| `darwinMain/.../AONCodec.kt` | 166 | actual: CommonCrypto pipeline |
| `darwinMain/.../CryptoDigest.kt` | 106 | actual: CC_ functions |
| `darwinMain/.../PlatformIdentity.kt` | 21 | actual: Bundle.main |
| `jsMain/.../AONCodec.kt` | 433 | actual: subtle/Node + pure Kotlin fallbacks |
| `jsMain/.../CryptoDigest.kt` | 274 | actual: runtime detection |
| `jsMain/.../PlatformIdentity.kt` | 71 | actual: origin/env |
| `commonTest/.../AONFormatTest.kt` | 241 | 23 cross-platform tests |
| **Total** | **~2,353** | |

---

## 11. Related

- **Plan**: `Docs/Plans/Crypto/Crypto-Plan-CrossPlatformAONSecurity-260223-V1.md`
- **Chapter 110**: Unified Command Architecture (IntentActions + Cockpit Dashboard)
- **AON Format in RAG**: `Modules/AI/RAG/src/androidMain/` — legacy Android-only AONFileManager
- **Next**: Chapter 112
