# Cross-Platform AON Security Plan — Unified Model Protection

> **Tracking**: `docs/plans/Crypto/Crypto-Plan-CrossPlatformAONSecurity-260223-V1.md`

## Context

The AON (AVA ONNX Naming) file format protects ML model files with HMAC signatures, package whitelisting, optional AES-256-GCM encryption, and integrity checks. Currently this security exists **only on Android** in `Modules/AI/RAG/src/androidMain/`. Five other platforms (iOS, Desktop JVM, JS/Browser, JS/Node.js, macOS native) have **zero protection** — they either read raw ONNX files or do a naive header strip.

This plan creates a unified cross-platform AON codec, extends the wrapping tool for all platforms, and adds JS-specific hardening (WASM key protection, RASP).

**Goal**: Every platform that loads an AVA model file gets HMAC verification, integrity checking, and platform-appropriate identity validation.

---

## Architecture Overview

```
Modules/Crypto/                              ← NEW KMP module
├── src/
│   ├── commonMain/                          ← AONFormat constants, interfaces
│   │   └── com/augmentalis/crypto/
│   │       ├── aon/
│   │       │   ├── AONFormat.kt             ← Header/footer layout, magic bytes, constants
│   │       │   ├── AONHeader.kt             ← Data class for parsed header
│   │       │   ├── AONFooter.kt             ← Data class for parsed footer
│   │       │   └── AONCodec.kt              ← expect class: wrap/unwrap/verify
│   │       ├── digest/
│   │       │   └── CryptoDigest.kt          ← expect: SHA-256, MD5, HMAC-SHA256, CRC32
│   │       └── identity/
│   │           └── PlatformIdentity.kt      ← expect: get platform app identity
│   ├── jvmMain/                             ← Shared Android + Desktop (javax.crypto)
│   │   └── com/augmentalis/crypto/
│   │       ├── aon/AONCodec.kt              ← actual: JVM wrap/unwrap/verify
│   │       ├── digest/CryptoDigest.kt       ← actual: MessageDigest, Mac, CRC32
│   │       └── identity/PlatformIdentity.kt ← actual: system property / process name
│   ├── androidMain/                         ← Android-specific identity only
│   │   └── com/augmentalis/crypto/
│   │       └── identity/PlatformIdentity.kt ← actual: context.packageName MD5
│   ├── iosMain/                             ← CommonCrypto via cinterop
│   │   └── com/augmentalis/crypto/
│   │       ├── aon/AONCodec.kt              ← actual: iOS wrap/unwrap/verify
│   │       ├── digest/CryptoDigest.kt       ← actual: CC_SHA256, CCHmac, CC_MD5
│   │       └── identity/PlatformIdentity.kt ← actual: Bundle.main.bundleIdentifier
│   ├── jsMain/                              ← crypto.subtle (browser) + crypto (Node.js)
│   │   └── com/augmentalis/crypto/
│   │       ├── aon/AONCodec.kt              ← actual: JS wrap/unwrap/verify
│   │       ├── digest/CryptoDigest.kt       ← actual: SubtleCrypto / Node crypto
│   │       └── identity/PlatformIdentity.kt ← actual: origin (browser) / process (Node)
│   └── macosMain/                           ← Same as iOS (CommonCrypto)
│       └── ... (symlink or shared with iosMain)
```

### Pattern: Follows VSMCodec (NOT expect/actual for codec)

Based on analysis of the VSMCodec pattern in SpeechRecognition (which produces byte-identical output across JVM + iOS):
- **AONFormat.kt** in `commonMain` — single source of truth for constants, header layout, byte helpers
- **CryptoDigest** uses `expect/actual` — lightweight, just wraps platform crypto primitives
- **PlatformIdentity** uses `expect/actual` — returns the platform's app identity string
- **AONCodec** uses `expect/actual` — the unwrap/verify logic references CryptoDigest internally

---

## Phase 1: New `Modules/Crypto/` KMP Module (~400 lines)

### 1A: Module setup

**New file**: `Modules/Crypto/build.gradle.kts`
- Targets: android, ios (x64/arm64/sim), macos (x64/arm64), jvm("desktop"), js(IR browser+nodejs)
- Dependencies: `kotlinx-coroutines-core` only
- iOS cinterop: `commoncrypto.def` (copy from SpeechRecognition)

### 1B: AONFormat.kt (commonMain, ~120 lines)

Single source of truth — extracted from current `AONFileManager.kt`:

```kotlin
object AONFormat {
    val MAGIC = byteArrayOf(0x41, 0x56, 0x41, 0x2D, 0x41, 0x4F, 0x4E, 0x01)  // "AVA-AON\x01"
    val FOOTER_MAGIC = byteArrayOf(0x45, 0x4E, 0x44, 0x41, 0x4F, 0x4E, 0x01, 0x00)
    const val FORMAT_VERSION = 1
    const val HEADER_SIZE = 256
    const val FOOTER_SIZE = 128
    const val MAX_PACKAGES = 3
    const val PACKAGE_HASH_SIZE = 16  // MD5 = 16 bytes
    // Header field offsets...
    // Byte conversion helpers (LE)...
}
```

Plus `AONHeader` and `AONFooter` data classes with all fields matching the current 256+128 byte layout.

### 1C: CryptoDigest.kt (expect/actual, ~200 lines total)

```kotlin
// commonMain
expect object CryptoDigest {
    fun sha256(data: ByteArray): ByteArray
    fun sha256Truncated16(data: ByteArray): ByteArray
    fun md5(data: ByteArray): ByteArray
    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray
    fun crc32(data: ByteArray): Int
}
```

| Platform | Implementation |
|----------|---------------|
| **jvmMain** (Android + Desktop) | `java.security.MessageDigest`, `javax.crypto.Mac`, `java.util.zip.CRC32` |
| **iosMain** / **macosMain** | `CC_SHA256`, `CCHmac(kCCHmacAlgSHA256)`, `CC_MD5` via cinterop |
| **jsMain** | Runtime detection: Node.js → `require('crypto')`, Browser → `crypto.subtle` |

JS detail — `crypto.subtle` is async (returns Promise), so the JS actual may need `suspend` variants or use synchronous Node.js `crypto.createHmac()` with a browser polyfill.

### 1D: PlatformIdentity.kt (expect/actual, ~100 lines total)

```kotlin
// commonMain
expect object PlatformIdentity {
    fun getAppIdentifier(): String    // Package name, bundle ID, origin, etc.
    fun getIdentityType(): String     // "android_package", "ios_bundle", "js_origin", etc.
}
```

| Platform | Identity Source |
|----------|---------------|
| **androidMain** | `context.packageName` (requires init with Context) |
| **iosMain** | `Bundle.main.bundleIdentifier` |
| **desktopMain** (via jvmMain) | `System.getProperty("aon.app.id")` or process name |
| **jsMain** (browser) | `window.location.origin` |
| **jsMain** (Node.js) | `process.env.AON_APP_ID` or `process.title` |

---

## Phase 2: AONCodec — Cross-Platform Unwrap + Verify (~300 lines)

### 2A: AONCodec.kt (expect/actual)

```kotlin
// commonMain
expect object AONCodec {
    /** Verify AON file integrity without unwrapping */
    fun verify(aonData: ByteArray): AONVerifyResult

    /** Unwrap AON to raw ONNX bytes with full verification */
    fun unwrap(aonData: ByteArray, appIdentifier: String? = null): ByteArray

    /** Check if data has AON magic bytes */
    fun isAON(data: ByteArray): Boolean

    /** Parse header without verification */
    fun parseHeader(data: ByteArray): AONHeader
}

data class AONVerifyResult(
    val valid: Boolean,
    val hmacValid: Boolean,
    val integrityValid: Boolean,     // SHA-256 + CRC32
    val identityValid: Boolean,      // Package/bundle/origin check
    val expired: Boolean,
    val modelId: String,
    val licenseTier: Int,            // 0=free, 1=pro, 2=enterprise
    val errors: List<String>
)
```

### 2B: Verification pipeline (all platforms)

Every `unwrap()` call performs these checks in order:

1. **Magic bytes** — first 8 bytes == `AVA-AON\x01`
2. **Format version** — bytes 8-11 == supported version
3. **HMAC-SHA256** — recompute signature over header (sig field zeroed) + SHA-256(ONNX data), compare to stored 64-byte signature
4. **SHA-256 integrity** — hash ONNX payload, compare first 16 bytes to header field + full 32 bytes to footer field
5. **CRC32 integrity** — compute CRC32(header + ONNX), compare to footer field
6. **Expiry check** — if expiryTimestamp > 0, check against current time
7. **Identity check** — if allowedPackages has non-zero entries, hash caller's identity with MD5, match against stored hashes
8. **AES-256-GCM decrypt** — if encryptionScheme == 1, decrypt ONNX data using derived key + stored IV

Fix for current Android gap: the existing `verifySignature()` skips HMAC recomputation because it doesn't retain raw header bytes. The new implementation stores raw header bytes and zeros the signature field for recomputation — **this fixes the v1 verification bug**.

### 2C: HMAC Key Strategy

**Current**: Hardcoded string `"AVA-AON-HMAC-SECRET-KEY-V1-CHANGE-IN-PRODUCTION"` with JVM system property override.

**New multi-platform approach**:

| Platform | Key Source | Fallback |
|----------|-----------|----------|
| Android | Android Keystore → system property → compiled constant | Compiled constant |
| iOS | Keychain → compiled constant | Compiled constant |
| Desktop JVM | System property `aon.hmac.key` → compiled constant | Compiled constant |
| Node.js | `process.env.AON_HMAC_KEY` → compiled constant | Compiled constant |
| Browser | WASM-embedded key (Phase 4) → compiled constant | Compiled constant |

The compiled constant is the **same key** across all platforms — this ensures an AON file created on any platform can be verified on any other platform. The key is obfuscated in source (split across multiple string fragments, XOR-masked) rather than stored as a single readable string.

---

## Phase 3: Update Consumers (~200 lines of changes)

### 3A: NLU ModelManager — all platforms

Wire up `AONCodec.unwrap()` in each platform's ModelManager:

| Platform | File | Change |
|----------|------|--------|
| **JS** | `Modules/AI/NLU/src/jsMain/.../ModelManager.kt` | Replace naive `unwrapAON()` with `AONCodec.unwrap()` |
| **Desktop** | `Modules/AI/NLU/src/desktopMain/.../ModelManager.kt` | Add `AONCodec.unwrap()` call when loading model file |
| **Android** | `Modules/AI/NLU/src/androidMain/.../ModelManager.kt` | Wire `AONCodec.unwrap()` (currently passes raw file path — needs to read bytes, unwrap, then pass to ONNX Runtime) |
| **iOS** | `Modules/AI/NLU/src/iosMain/.../ModelManager.kt` | Add `AONCodec.unwrap()` call |

### 3B: NLU build.gradle.kts

Add dependency: `implementation(project(":Modules:Crypto"))`

### 3C: RAG module migration (future)

The existing `AONFileManager` in RAG can eventually delegate to `Modules/Crypto/` for the core crypto. Not part of this plan — the new module is additive, not a replacement.

---

## Phase 4: JS Hardening — WASM + RASP (~250 lines)

Based on the research from the Google AI mode link:

### 4A: WASM Key Protection

Move the HMAC key material and verification logic into a WebAssembly module:

**New file**: `Modules/Crypto/src/jsMain/resources/aon-verify.wasm` (compiled from C/Rust)

```
// Pseudo-C source (compiled to WASM offline)
int verify_hmac(uint8_t* header, uint8_t* onnx_hash, uint8_t* expected_sig) {
    // HMAC key embedded in compiled WASM binary
    // Key is split across multiple functions, XOR-masked
    // Compute HMAC-SHA256 and compare
}
```

**Why WASM**: The key is in compiled binary format — much harder to extract than a string in minified JS. It's the "compiling alternative" from the research.

**Build**: Use `emscripten` or Rust `wasm-pack` to compile a tiny HMAC-SHA256 verifier (~10KB WASM). The WASM binary ships alongside the JS module.

**JS integration**:
```kotlin
// In jsMain AONCodec
val wasmModule = WebAssembly.instantiate(wasmBytes)
val isValid = wasmModule.exports.verify_hmac(headerBytes, onnxHash, expectedSig)
```

### 4B: RASP (Runtime Application Self-Protection)

Add to `Modules/Crypto/src/jsMain/`:

**Integrity self-check**: The JS module computes a hash of its own source code at startup. If the hash doesn't match the expected value (embedded at build time), the verification functions return `false`.

**Anti-tampering**: The `unwrap()` function checks if `AONCodec` methods have been monkey-patched:
```kotlin
// Detect if verify() has been replaced
val originalVerify = AONCodec::verify
if (originalVerify !== storedReference) {
    // Tampered — refuse to unwrap
}
```

**Domain locking (browser only)**: If `window.location.origin` doesn't match an allowed list, refuse to unwrap. This is the web equivalent of Android package whitelisting.

### 4C: String concealing

The HMAC fallback key in JS is never stored as a single string literal. Instead:
```kotlin
private fun getKey(): ByteArray {
    val a = intArrayOf(0x41, 0x56, 0x41, 0x2D)  // Split across fragments
    val b = intArrayOf(0x41, 0x4F, 0x4E, 0x2D)
    val mask = intArrayOf(0x12, 0x34, 0x56, 0x78) // XOR mask
    // Reassemble at runtime, XOR to decode
    return (a + b).mapIndexed { i, v -> (v xor mask[i % mask.size]).toByte() }.toByteArray()
}
```

This survives minification — the key is never a greppable string in the bundle.

---

## Phase 5: Unified Wrapping Tool (~300 lines)

### 5A: Extend AONWrapperTool

**File**: `Modules/Crypto/src/desktopMain/.../cli/AONWrapperTool.kt`

Move the wrapping tool to the Crypto module and add multi-platform support:

```bash
# Wrap for all platforms (default)
./gradlew :Modules:Crypto:runAONWrapper \
  --args="wrap --input=model.onnx --output=AVA-384-NLU-INT8.AON \
          --model-id=AVA-384-NLU-INT8 \
          --platforms=android,ios,desktop,web"

# Wrap with platform-specific identity lists
./gradlew :Modules:Crypto:runAONWrapper \
  --args="wrap --input=model.onnx --output=AVA-384-NLU-INT8.AON \
          --model-id=AVA-384-NLU-INT8 \
          --android-packages=com.augmentalis.ava,com.augmentalis.voiceos \
          --ios-bundles=com.augmentalis.ava,com.augmentalis.voiceos \
          --web-origins=https://ava.augmentalis.com,https://app.avanues.com"
```

### 5B: Multi-platform identity in header

The current header has 48 bytes for 3 MD5 package hashes. For cross-platform support, repurpose the `reserved` fields:

| Field | Offset | Size | Content |
|-------|--------|------|---------|
| `allowedPackages` | 208 | 48 | 3 x MD5 hashes (Android package names) — **unchanged** |
| `reserved1` (repurposed) | 129 | 15 | 1-byte platform flags + 14 bytes for web origin hash |

**Platform flags byte** (offset 129):
- Bit 0: Android allowed
- Bit 1: iOS allowed
- Bit 2: Desktop allowed
- Bit 3: Web allowed
- Bit 4: Node.js allowed
- Bits 5-7: Reserved

When platform flag is set but no identity hash matches → allow (trust the platform flag). When identity hashes ARE present → must match. This is backward compatible — existing AON files with `reserved1 = 0x00` work on all platforms.

### 5C: Batch wrapping for NLU models

Add NLU models to the `BatchModelWrapper` MODEL_REGISTRY:

```kotlin
ModelRegistryEntry("AVA-384-NLU-INT8", "mobilebert_model.onnx", licenseTier = 0),
ModelRegistryEntry("AVA-768-NLU-MULTI-INT8", "malbert_model.onnx", licenseTier = 0),
```

---

## Phase 6: Tests (~200 lines)

### 6A: commonTest — format verification
- Parse header/footer round-trip
- Magic byte detection
- AON vs raw ONNX detection

### 6B: Platform tests
- **jvmTest**: HMAC computation, SHA-256, CRC32, full wrap/unwrap round-trip
- **jsTest**: crypto.subtle HMAC, Node.js crypto, browser SubtleCrypto mock
- **iosTest**: CommonCrypto HMAC, round-trip verification

### 6C: Cross-platform byte-identical test
- Wrap on JVM → unwrap on JS → verify identical ONNX bytes
- Wrap on JVM → unwrap on iOS → verify identical ONNX bytes

---

## Files Modified / Created

| File | Action | Est. Lines |
|------|--------|-----------|
| `Modules/Crypto/build.gradle.kts` | **NEW** | ~80 |
| `Modules/Crypto/src/commonMain/.../aon/AONFormat.kt` | **NEW** | ~120 |
| `Modules/Crypto/src/commonMain/.../aon/AONCodec.kt` | **NEW** (expect) | ~60 |
| `Modules/Crypto/src/commonMain/.../digest/CryptoDigest.kt` | **NEW** (expect) | ~30 |
| `Modules/Crypto/src/commonMain/.../identity/PlatformIdentity.kt` | **NEW** (expect) | ~20 |
| `Modules/Crypto/src/jvmMain/.../aon/AONCodec.kt` | **NEW** (actual) | ~200 |
| `Modules/Crypto/src/jvmMain/.../digest/CryptoDigest.kt` | **NEW** (actual) | ~60 |
| `Modules/Crypto/src/jvmMain/.../identity/PlatformIdentity.kt` | **NEW** (actual) | ~30 |
| `Modules/Crypto/src/androidMain/.../identity/PlatformIdentity.kt` | **NEW** (actual) | ~30 |
| `Modules/Crypto/src/iosMain/.../aon/AONCodec.kt` | **NEW** (actual) | ~180 |
| `Modules/Crypto/src/iosMain/.../digest/CryptoDigest.kt` | **NEW** (actual) | ~60 |
| `Modules/Crypto/src/iosMain/.../identity/PlatformIdentity.kt` | **NEW** (actual) | ~20 |
| `Modules/Crypto/src/jsMain/.../aon/AONCodec.kt` | **NEW** (actual) | ~220 |
| `Modules/Crypto/src/jsMain/.../digest/CryptoDigest.kt` | **NEW** (actual) | ~80 |
| `Modules/Crypto/src/jsMain/.../identity/PlatformIdentity.kt` | **NEW** (actual) | ~40 |
| `Modules/Crypto/src/jsMain/.../aon/WasmVerifier.kt` | **NEW** | ~60 |
| `Modules/Crypto/src/jsMain/.../rasp/IntegrityCheck.kt` | **NEW** | ~50 |
| `Modules/Crypto/src/desktopMain/.../cli/AONWrapperTool.kt` | **NEW** (moved+extended) | ~300 |
| `Modules/Crypto/src/nativeInterop/cinterop/commoncrypto.def` | **NEW** (copy from SpeechRecognition) | ~4 |
| `Modules/AI/NLU/build.gradle.kts` | MODIFY — add Crypto dep | ~2 |
| `Modules/AI/NLU/src/jsMain/.../ModelManager.kt` | MODIFY — use AONCodec | ~10 |
| `Modules/AI/NLU/src/desktopMain/.../ModelManager.kt` | MODIFY — use AONCodec | ~15 |
| `Modules/AI/NLU/src/androidMain/.../ModelManager.kt` | MODIFY — use AONCodec | ~15 |
| `settings.gradle.kts` | MODIFY — include Crypto module | ~1 |
| Tests (commonTest + platform tests) | **NEW** | ~200 |

**Total**: ~1,900 lines new/modified code

---

## Security Comparison: Before vs After

| Aspect | Before (Android only) | After (All platforms) |
|--------|----------------------|----------------------|
| HMAC verification | Android (broken — sig field not zeroed) | All 6 platforms (fixed) |
| SHA-256 integrity | Android only | All platforms |
| CRC32 integrity | Android only | All platforms |
| Package/identity check | Android (3 MD5 hashes) | Android + iOS + Desktop + Web (origin) |
| AES-256-GCM encryption | Android only | All platforms |
| Expiry enforcement | Android only | All platforms |
| Key protection (JS) | N/A | WASM-embedded key + string concealing |
| Anti-tamper (JS) | N/A | RASP integrity checks |
| Domain locking (Web) | N/A | Origin whitelist check |

---

## Risk Summary

| Risk | Severity | Mitigation |
|------|----------|------------|
| `crypto.subtle` is async-only in browsers | Medium | Use sync Node.js `crypto` when available; for browser, make `unwrap()` suspending |
| WASM binary adds ~10KB to JS bundle | Low | Acceptable; lazy-loaded only when AON file detected |
| iOS cinterop complexity | Medium | Copy proven pattern from SpeechRecognition VSMCodec |
| Backward compatibility with existing AON files | High | Platform flags default to 0x00 = allow all; HMAC key unchanged |
| HMAC key in compiled constant still extractable | Medium | Defense in depth: WASM + string concealing + RASP raises the bar significantly |
| Native bridge not used for WebView contexts | Low | Future: WebAvanue (Android WebView) can use `addJavascriptInterface` to delegate HMAC to native JVM; iOS WebView can use `WKScriptMessageHandler` to delegate to CommonCrypto. Not needed for Phase 1 since JS target is standalone browser/Node.js, not WebView-embedded |

---

## Future: Native Bridge Hardening (Post-Phase 6)

For apps that embed web content in native WebViews (e.g., WebAvanue on Android/iOS), the HMAC verification can be delegated to the native side via platform bridges rather than running in JS:

| Platform | Bridge | Benefit |
|----------|--------|---------|
| Android WebView | `addJavascriptInterface` (API 17+) | HMAC key never in JS context; native `javax.crypto` handles verification |
| iOS WKWebView | `WKScriptMessageHandler` | Key in iOS Keychain; `CCHmac` handles verification |
| Electron | Node.js `crypto` (already in plan) | Sync crypto, no extractable key issue |
| Tauri | Rust `invoke()` command | Key in compiled Rust binary, strongest protection |

This is **not part of the initial implementation** because:
1. Our KMP JS target is standalone browser/Node.js, not WebView-embedded
2. Phase 4 (WASM + RASP) provides strong standalone browser protection
3. Native bridge integration requires per-app WebView setup

**When to add**: When WebAvanue or other WebView-embedded contexts need to load AON models directly.

---

## Verification

1. `./gradlew :Modules:Crypto:allTests` — all platform tests pass
2. Wrap a test ONNX file on Desktop JVM → unwrap on JS (browser) → verify identical bytes
3. Wrap with package whitelist → unwrap from unauthorized package → verify rejection
4. Wrap with expiry → advance clock past expiry → verify rejection
5. Corrupt 1 byte of ONNX data in AON file → unwrap → verify HMAC/integrity failure
6. `./gradlew :Modules:AI:NLU:compileKotlinJs :Modules:AI:NLU:compileKotlinDesktop :Modules:AI:NLU:compileDebugKotlinAndroid` — all NLU targets compile
7. Load model in browser → verify HMAC passes → ONNX Runtime inference works
