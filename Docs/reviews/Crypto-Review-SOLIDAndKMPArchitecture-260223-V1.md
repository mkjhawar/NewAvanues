# Crypto Module Architecture Review — SOLID & KMP Analysis
**Date:** 260223
**Scope:** `Modules/Crypto/` + NLU consumer integration
**Reviewer:** Code-Reviewer Agent
**Files Reviewed:** 17 source files (Crypto module) + 4 NLU ModelManager files + 2 build files

---

## Summary

`Modules/Crypto/` is a well-structured KMP cryptographic module with a correct source-set hierarchy, complete expect/actual coverage across all targets, and solid single-responsibility design. The primary issues are a security-class concern (hardcoded HMAC key shipped in production binaries with only XOR obfuscation), two runtime bugs (AES key derived from HMAC key on JVM and JS violating key separation; iOS silently throws on encrypted models), duplicated verification logic across three platform implementations that violates DRY, and a concurrency hazard in the Android `ModelManager` consumer. SOLID scores are high overall, dragged down only by the OCP limitation (adding a cipher requires modifying every platform's `AONCodec`).

---

## Source Set Hierarchy Verification

| Source Set | Files | Correctness |
|-----------|-------|-------------|
| `commonMain` | `AONCodec.kt` (expect), `CryptoDigest.kt` (expect), `PlatformIdentity.kt` (expect), `AONFormat.kt`, `AONHeader.kt`, `AONFooter.kt` | Correct |
| `jvmMain` | `AONCodec.kt` (actual), `CryptoDigest.kt` (actual) | Correct — shared by androidMain + desktopMain |
| `androidMain` | `PlatformIdentity.kt` (actual) | Correct — Android-specific context.packageName |
| `desktopMain` | `PlatformIdentity.kt` (actual) | Correct — inherits jvmMain AONCodec + CryptoDigest |
| `darwinMain` | `AONCodec.kt` (actual), `CryptoDigest.kt` (actual), `PlatformIdentity.kt` (actual) | Correct — shared by iosMain + macosMain |
| `jsMain` | `AONCodec.kt` (actual), `CryptoDigest.kt` (actual), `PlatformIdentity.kt` (actual) | Correct |

**No redeclaration issues exist.** `jvmMain` provides `AONCodec` and `CryptoDigest` for both `androidMain` and `desktopMain` via the `dependsOn(jvmMain)` chain. `PlatformIdentity` is separately implemented in `androidMain` and `desktopMain` to diverge on identity source, which is correct.

---

## expect/actual Completeness Matrix

| expect function | jvmMain (Android+Desktop) | darwinMain (iOS+macOS) | jsMain |
|----------------|--------------------------|----------------------|--------|
| `AONCodec.verify()` | actual (suspend) | actual (suspend) | actual (suspend) |
| `AONCodec.unwrap()` | actual (suspend) | actual (suspend — throws for encrypted) | actual (suspend) |
| `AONCodec.isAON()` | actual (non-suspend) | actual (non-suspend) | actual (non-suspend) |
| `AONCodec.parseHeader()` | actual (non-suspend) | actual (non-suspend) | actual (non-suspend) |
| `CryptoDigest.sha256()` | actual (suspend) | actual (suspend) | actual (suspend) |
| `CryptoDigest.sha256Truncated16()` | actual (suspend) | actual (suspend) | actual (suspend) |
| `CryptoDigest.md5()` | actual (suspend) | actual (suspend) | actual (suspend, pure-Kotlin browser fallback) |
| `CryptoDigest.hmacSha256()` | actual (suspend) | actual (suspend) | actual (suspend) |
| `CryptoDigest.crc32(ByteArray)` | actual (non-suspend) | actual (non-suspend) | actual (non-suspend) |
| `CryptoDigest.crc32(vararg)` | actual (non-suspend) | actual (non-suspend) | actual (non-suspend) |
| `PlatformIdentity.getAppIdentifier()` | actual (androidMain + desktopMain) | actual (darwinMain) | actual (jsMain) |
| `PlatformIdentity.getIdentityType()` | actual (androidMain + desktopMain) | actual (darwinMain) | actual (jsMain) |

**All expect declarations have actuals on every target. suspend/non-suspend consistency is correct throughout.**

---

## Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| Critical | `commonMain/.../aon/AONFormat.kt:194-210` | `getDefaultHmacKey()` ships a hardcoded HMAC key in every binary, protected only by XOR obfuscation with a constant mask. Any decompiler or strings scan recovers it. The comment says "CHANGE-IN-PRODUCTION" but there is no enforcement mechanism — this is the production key. | Replace with a key derivation path that pulls the signing key from Android Keystore / iOS Keychain at runtime. The default must not exist in release builds — use a build-flavour guard or a required injection point. |
| Critical | `jvmMain/.../aon/AONCodec.kt:199` | `decryptAesGcm` calls `CryptoDigest.sha256Blocking(AONFormat.getDefaultHmacKey())` to derive the AES-GCM key — using the HMAC signing key as AES encryption key material. Key separation violation: compromising the encryption key also breaks authentication. Additionally `sha256Blocking` bypasses the `suspend` `CryptoDigest` interface, creating a separate SHA-256 code path. | Define a separate AES key secret. Restructure `decryptAesGcm` to be `suspend` and call `CryptoDigest.sha256()` directly. Remove `sha256Blocking`. |
| Critical | `jsMain/.../aon/AONCodec.kt:222,246` | Same key-separation violation as JVM: `decryptAesGcmNode` and `decryptAesGcmBrowser` derive the AES key by hashing the HMAC key. | Same fix as JVM — define `AONFormat.getDefaultAesKey()` as an independent secret (or derive both via HKDF from a master secret with distinct labels `"aon-hmac-v1"` and `"aon-aes-v1"`). |
| High | `darwinMain/.../aon/AONCodec.kt:120-122` | iOS silently throws `AONSecurityException("AES-256-GCM decryption not yet supported on iOS")` for encrypted AON files. The `expect` signature does not document this platform limitation. Callers in commonMain cannot know iOS will throw for encrypted models. | Either document this in KDoc on the expect declaration explicitly, or implement CommonCrypto-based AES-GCM via `CCCryptorGCM` (available since iOS 13). |
| High | `androidMain/.../ModelManager.kt:277-279` | `isModelAvailable()` calls `detectBestModel()` unconditionally every time. `detectBestModel()` is also called from `getModelPath()` (L313), `copyModelFromAssetsInternal()` (L418), `copyModelFromAssets()` (L512). Multiple callers chain these — e.g., `downloadModelsIfNeeded` triggers multiple redundant filesystem walks per user action. | Cache detection result. Skip re-detection when `activeModelType != null`. Invalidate only on model change events (copy complete, clear). |
| High | `androidMain/.../ModelManager.kt:43-44` | `externalModelsDir = File("/sdcard/ava-ai-models/embeddings")` — hardcoded `/sdcard` path violates Android scoped storage (mandatory since Android 10 / API 29). The module declares `minSdk = 28`, so API 30+ devices will be blocked. | Use `context.getExternalFilesDir(null)` for app-private external storage (no dangerous permission required), or `Environment.getExternalStoragePublicDirectory()` with a scoped subdirectory. |
| High | `androidMain/.../ModelManager.kt:683-728` | `calculateModelChecksum()` directly uses `java.security.MessageDigest.getInstance("SHA-256")`, duplicating the JVM SHA-256 path already in `jvmMain/CryptoDigest`. Two independent SHA-256 code paths on Android. | Call `CryptoDigest.sha256()` or make `calculateModelChecksum` a `suspend` function. |
| Medium | `jvmMain/AONCodec.kt`, `darwinMain/AONCodec.kt`, `jsMain/AONCodec.kt` — `verify()` | The 8-step verification pipeline is duplicated verbatim across three platform implementations (~85 lines each, ~255 lines total). Steps 1-7 are platform-independent; only step 8 (AES-GCM + clock) differs. | Extract steps 1-7 into a `commonMain` `internal fun buildVerifyResult(...)`. Each platform `actual AONCodec.verify()` delegates to it and supplies only the platform clock and decrypt step. |
| Medium | Same three `AONCodec` actuals — `verifyHmac()`, `verifyIdentity()` | Both private helpers are duplicated in each platform implementation with identical signatures and near-identical logic. | Move `verifyIdentity()` to `commonMain` (it only calls `expect` objects). Move the canonical HMAC verification algorithm to `commonMain` parameterised by key. |
| Medium | `desktopMain/ModelManager.kt:440-469` and `jsMain/ModelManager.kt:401-429` | `ModelType` enum declared independently in both source sets with identical values. | Move `ModelType` to `commonMain`. It has no platform-specific API surface. |
| Medium | `commonMain/.../aon/AONFormat.kt:63` | `AONFormat` mixes three responsibilities: (1) format constants/offsets, (2) binary serialization helpers, (3) HMAC key management. | Split into `AONConstants` (offsets, magic, flags), `AONSerializer` (put/get helpers), and a separate key provider. |
| Medium | `jsMain/.../aon/AONCodec.kt:219-402` | A full 80-line pure-Kotlin SHA-256 implementation (`sha256Pure`) is embedded inside `AONCodec.kt`, mixing codec and digest responsibilities. | Move `sha256Pure` to `jsMain/CryptoDigest.kt` as a private helper. |
| Medium | `jsMain/CryptoDigest.kt:252-258` and `darwinMain/CryptoDigest.kt:80-90` | CRC32 lookup table and `updateCrc32()` are identical in both files (same polynomial, same algorithm). | Extract to a `nonJvmMain` or shared intermediate source set that both `darwinMain` and `jsMain` depend on. |
| Medium | `androidMain/ModelManager.kt:48-52` | `packageDataPaths` hardcodes `com.augmentalis.ava` and `com.augmentalis.ava.debug` inside a shared library module. If any other AVA app uses `ModelManager`, the hardcoded paths create incorrect search behavior and misleading logs. | Remove hardcoded package IDs. Accept additional search paths via constructor parameter. |
| Low | `commonMain/.../aon/AONHeader.kt:81-94` and `AONFooter.kt:60-73` | `equals()` on `AONHeader` compares only `modelId`, `formatVersion`, and `modelVersion`. Two headers with different HMAC signatures, expiry timestamps, or allowed packages are considered equal. | Either remove the custom `equals`/`hashCode` overrides (rely on data class identity) or expand to include `signature.contentEquals()` and `expiryTimestamp`. |
| Low | `jsMain/AONCodec.kt:29-31`, `jsMain/CryptoDigest.kt:29-31`, `jsMain/PlatformIdentity.kt:21-23` | The `isNodeJs` detection expression is duplicated in three separate JS files. | Extract to a top-level `internal val isNodeJs: Boolean` in a shared `jsMain/JsPlatform.kt`. |
| Low | `androidMain/.../identity/PlatformIdentity.kt:26-28` | `init(context)` must be called manually before `AONCodec` is used. If forgotten, identity falls back silently to `"android.unknown"`, causing AON identity checks to fail for package-restricted files with no error at the init call site. | Add `check(packageName != null) { "PlatformIdentity.init(context) must be called before AONCodec" }` at the top of `getAppIdentifier()`. |
| Low | `desktopMain/ModelManager.kt:395-397` | `getActiveModelType()` unconditionally returns `ModelType.MOBILEBERT` regardless of which file `detectBestModel()` found. If `AVA-768-Base-INT8.AON` is present, the active model is mALBERT but `getActiveModelType()` reports MOBILEBERT. | Track the detected `ModelType` in a field (as Android does) and return it from `getActiveModelType()`. |
| Low | `jsMain/ModelManager.kt:160-168` | `copyModelFromAssets()` delegates to `downloadModelsIfNeeded()` with no `onProgress` callback — a CDN download fires with no progress reporting. Intent is unclear from the call site. | Document explicitly in KDoc that on JS this triggers a download, not a local copy. |

---

## SOLID Scores

| Principle | Score | Rationale |
|-----------|-------|-----------|
| **SRP** | 6/10 | `AONFormat` mixes constants, serialization helpers, and key management. `jsMain/AONCodec` mixes format verification with a full SHA-256 implementation. |
| **OCP** | 5/10 | Adding a new encryption scheme (e.g., ChaCha20-Poly1305) requires modifying `AONFormat.kt` (new constant), every `AONCodec.kt` actual (new branch in `unwrap`), and the Darwin limitation comment. No extension point exists. |
| **LSP** | 8/10 | All actuals fulfill the expect contract for 3 of 4 functions. iOS `unwrap()` throws for encrypted files — a documented behavioral restriction, not a type safety violation, but callers expecting parity with JVM are surprised. |
| **ISP** | 9/10 | `CryptoDigest` exposes exactly what is needed. `PlatformIdentity` has two focused methods. `AONCodec` has four, all consumed together. No fat interfaces. |
| **DIP** | 7/10 | `AONCodec.actual` on JVM and Darwin depend on `CryptoDigest` and `PlatformIdentity` abstractions correctly. Weakness: `decryptAesGcm` bypasses `CryptoDigest.sha256()` by calling `sha256Blocking` directly, coupling to concrete `MessageDigest`. |

---

## Finding Counts

| Severity | Count |
|----------|-------|
| Critical | 3 |
| High | 4 |
| Medium | 6 |
| Low | 6 |
| **Total** | **19** |

---

## Recommendations (Priority Order)

1. **Fix key separation (Critical, immediate)**: `decryptAesGcm` on JVM and JS must not derive the AES key from the HMAC signing key. Define a separate AES key constant, or derive both via HKDF from a master secret with distinct labels.

2. **Replace hardcoded HMAC key (Critical, before release)**: `getDefaultHmacKey()` must be replaced by platform Keystore/Keychain injection. Consider an injectable `AONKeyProvider` interface: test builds pass a known key; production builds are forced to wire a secure store. This also resolves the `sha256Blocking` bypass.

3. **Implement AES-GCM on iOS (High, near-term)**: `CommonCrypto` provides `CCCryptorGCM` available since iOS 13. The encryption capability gap is a real user-facing limitation.

4. **Extract the shared verification pipeline (Medium, high ROI)**: Triplicated `verify()` logic is the highest-impact maintainability fix. A bug fix applied to one copy currently must be manually replicated to three.

5. **Fix Android scoped storage (High, before API 30+ testing)**: Replace `/sdcard/ava-ai-models/` with `context.getExternalFilesDir(null)`.

6. **Move `ModelType` to `commonMain` (Medium, trivial)**: Prevents future enum drift across desktop and JS.

7. **Add `PlatformIdentity.init()` guard (Low, defensive)**: Prevents silent identity fallback from masking missing initialization.

8. **Extract Node.js detection constant (Low, trivial)**: Single `jsMain/JsPlatform.kt` file eliminates three-file duplication of `isNodeJs`.
