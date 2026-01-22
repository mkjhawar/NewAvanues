# Implementation Plan: Plugin System Phase 6 - Advanced Features

## Overview
| Item | Value |
|------|-------|
| Platforms | KMP (commonMain), Android, iOS, Desktop |
| Swarm Recommended | **Yes** (20+ tasks, 3 major features) |
| Total Tasks | 24 |
| Dependencies | Phase 5 complete (PluginHotReloader, RemotePluginDiscovery) |

## Chain of Thought: Phase Ordering

**Why this order:**
1. **Hot Reload First** → Foundation for developer experience, needed to test security/marketplace iterations
2. **Security Second** → Required before marketplace (can't distribute untrusted plugins)
3. **Marketplace Last** → Depends on both hot reload (for updates) and security (for verification)

**KMP Decision:** All code in `commonMain` for maximum reuse (Android + iOS + Desktop)

---

## Phase 1: Hot Reload Full Implementation

**Goal:** Enable complete plugin unload → load cycle with state preservation

### Task 1.1: Add ServiceEndpoint Storage to Registry
- **File:** `UniversalPluginRegistry.kt`
- **Changes:**
  - Store `ServiceEndpoint` alongside `PluginRegistration`
  - Add `getEndpoint(pluginId)` method
  - Add `updateEndpoint()` for endpoint changes during reload

### Task 1.2: Create Plugin State Serialization Interface
- **File:** `plugins/universal/PluginStateSerializer.kt` (NEW)
- **Interface:**
  ```kotlin
  interface PluginStateSerializer {
      fun serializeState(plugin: UniversalPlugin): ByteArray
      fun deserializeState(data: ByteArray): Map<String, Any>
  }
  ```

### Task 1.3: Extend UniversalPlugin with State Methods
- **File:** `UniversalPlugin.kt`
- **Add methods:**
  ```kotlin
  suspend fun saveState(): Map<String, Any>
  suspend fun restoreState(state: Map<String, Any>): Result<Unit>
  ```

### Task 1.4: Implement Full Reload Cycle in PluginHotReloader
- **File:** `hotreload/PluginHotReloader.kt`
- **Changes:**
  - Add `reloadPlugin(pluginId)` method
  - Implement: saveState → shutdown → unregister → rediscover → register → initialize → restoreState
  - Add rollback on failure

### Task 1.5: Add State Persistence Storage
- **File:** `hotreload/PluginStateStorage.kt` (NEW)
- **Features:**
  - Save state to filesystem before reload
  - Load state after reload
  - Cleanup old state files
  - Encryption for sensitive state

### Task 1.6: Create Hot Reload Tests
- **File:** `commonTest/.../hotreload/PluginHotReloaderTest.kt` (NEW)
- **Tests:**
  - Reload with state preservation
  - Rollback on failure
  - Concurrent reload handling

---

## Phase 2: Remote Plugin Security

**Goal:** Implement sandboxing, signature verification, and permission escalation detection

### Task 2.1: Implement SignatureVerifier for JVM/Android
- **File:** `plugins/security/SignatureVerifierJvm.kt` (NEW - jvmMain/androidMain)
- **Implement:** `expect class SignatureVerifier` actual
- **Features:**
  - RSA-SHA256, RSA-SHA512, ECDSA verification
  - PEM/DER key loading
  - External and embedded signatures

### Task 2.2: Implement SignatureVerifier for iOS
- **File:** `plugins/security/SignatureVerifierIos.kt` (NEW - iosMain)
- **Features:**
  - Security framework integration
  - CommonCrypto usage

### Task 2.3: Create Plugin Sandbox Interface
- **File:** `plugins/security/PluginSandbox.kt` (NEW)
- **Interface:**
  ```kotlin
  interface PluginSandbox {
      fun checkPermission(pluginId: String, permission: Permission): Boolean
      fun enforcePermission(pluginId: String, permission: Permission)
      fun getGrantedPermissions(pluginId: String): Set<Permission>
  }
  ```

### Task 2.4: Implement Permission Manifest Parser
- **File:** `plugins/security/PermissionManifestParser.kt` (NEW)
- **Features:**
  - Parse required permissions from plugin manifest
  - Validate permission syntax
  - Check for dangerous permission combinations

### Task 2.5: Create Permission Escalation Detector
- **File:** `plugins/security/PermissionEscalationDetector.kt` (NEW)
- **Features:**
  - Detect runtime permission requests beyond manifest
  - Alert on privilege escalation attempts
  - Block unauthorized capability usage

### Task 2.6: Integrate Security into Plugin Loading
- **File:** `plugins/discovery/RemotePluginDiscovery.kt`
- **Changes:**
  - Verify signature before loading
  - Check TrustStore for publisher
  - Validate permissions before activation

### Task 2.7: Add Security Event Logging
- **File:** `plugins/security/SecurityAuditLogger.kt` (NEW)
- **Features:**
  - Log all security-relevant events
  - Signature verification results
  - Permission checks
  - Escalation attempts

### Task 2.8: Create Security Tests
- **File:** `commonTest/.../security/SecurityIntegrationTest.kt` (NEW)
- **Tests:**
  - Signature verification (valid/invalid)
  - Permission checking
  - Escalation detection
  - TrustStore operations

---

## Phase 3: Plugin Marketplace

**Goal:** Define submission format, validation rules, and discovery API

### Task 3.1: Define Plugin Package Format
- **File:** `plugins/marketplace/PluginPackageSpec.kt` (NEW)
- **Spec:**
  ```kotlin
  data class PluginPackage(
      val manifest: PluginManifest,
      val signature: String,
      val assets: List<AssetRef>,
      val checksum: String,
      val publishedAt: Long
  )
  ```

### Task 3.2: Create Submission Validator
- **File:** `plugins/marketplace/SubmissionValidator.kt` (NEW)
- **Validation Rules:**
  - Manifest completeness
  - Version format (semver)
  - Required fields present
  - Icon/screenshots provided
  - Signature valid
  - No malicious patterns

### Task 3.3: Define Discovery API Contract
- **File:** `plugins/marketplace/MarketplaceApi.kt` (NEW)
- **Endpoints:**
  ```kotlin
  interface MarketplaceApi {
      suspend fun search(query: String, filters: SearchFilters): List<PluginListing>
      suspend fun getDetails(pluginId: String): PluginDetails
      suspend fun getVersions(pluginId: String): List<VersionInfo>
      suspend fun download(pluginId: String, version: String): PluginPackage
      suspend fun checkUpdates(installed: Map<String, String>): List<UpdateInfo>
  }
  ```

### Task 3.4: Create Plugin Listing Data Model
- **File:** `plugins/marketplace/PluginListing.kt` (NEW)
- **Model:**
  ```kotlin
  data class PluginListing(
      val pluginId: String,
      val name: String,
      val description: String,
      val author: PublisherInfo,
      val version: String,
      val rating: Float,
      val downloadCount: Long,
      val capabilities: List<String>,
      val iconUrl: String,
      val screenshots: List<String>
  )
  ```

### Task 3.5: Implement Local Marketplace Cache
- **File:** `plugins/marketplace/MarketplaceCache.kt` (NEW)
- **Features:**
  - Cache catalog locally
  - Incremental updates
  - Offline browsing
  - Cache invalidation

### Task 3.6: Create Marketplace Client Implementation
- **File:** `plugins/marketplace/MarketplaceClientImpl.kt` (NEW)
- **Features:**
  - HTTP client integration
  - Rate limiting
  - Retry with backoff
  - Response caching

### Task 3.7: Integrate with RemotePluginDiscovery
- **File:** `plugins/discovery/RemotePluginDiscovery.kt`
- **Changes:**
  - Add marketplace as catalog source
  - Unified update checking
  - Download via marketplace client

### Task 3.8: Add Update Notification System
- **File:** `plugins/marketplace/UpdateNotifier.kt` (NEW)
- **Features:**
  - Background update checking
  - User notification
  - Auto-update support (optional)

### Task 3.9: Create Marketplace UI Data Provider
- **File:** `plugins/marketplace/MarketplaceDataProvider.kt` (NEW)
- **Features:**
  - Search results for UI
  - Plugin details for UI
  - Install progress tracking

### Task 3.10: Create Marketplace Tests
- **File:** `commonTest/.../marketplace/MarketplaceIntegrationTest.kt` (NEW)
- **Tests:**
  - Search functionality
  - Package validation
  - Download and install
  - Update detection

---

## Time Estimates

| Mode | Estimate |
|------|----------|
| Sequential | 24 tasks |
| Parallel (Swarm) | ~3 parallel tracks |

### Swarm Agent Allocation
| Agent | Tasks |
|-------|-------|
| Hot Reload Agent | Phase 1 (Tasks 1.1-1.6) |
| Security Agent | Phase 2 (Tasks 2.1-2.8) |
| Marketplace Agent | Phase 3 (Tasks 3.1-3.10) |

---

## Quality Gates

| Gate | Requirement |
|------|-------------|
| Tests | 90%+ coverage for new code |
| Build | Must compile for Android, iOS, JVM |
| Security | No hardcoded credentials, proper encryption |
| Lint | 0 errors, 0 warnings |

---

## Files Summary

### New Files (19)
| Phase | File |
|-------|------|
| 1 | `plugins/universal/PluginStateSerializer.kt` |
| 1 | `plugins/hotreload/PluginStateStorage.kt` |
| 1 | `commonTest/.../hotreload/PluginHotReloaderTest.kt` |
| 2 | `plugins/security/SignatureVerifierJvm.kt` (androidMain) |
| 2 | `plugins/security/SignatureVerifierIos.kt` (iosMain) |
| 2 | `plugins/security/PluginSandbox.kt` |
| 2 | `plugins/security/PermissionManifestParser.kt` |
| 2 | `plugins/security/PermissionEscalationDetector.kt` |
| 2 | `plugins/security/SecurityAuditLogger.kt` |
| 2 | `commonTest/.../security/SecurityIntegrationTest.kt` |
| 3 | `plugins/marketplace/PluginPackageSpec.kt` |
| 3 | `plugins/marketplace/SubmissionValidator.kt` |
| 3 | `plugins/marketplace/MarketplaceApi.kt` |
| 3 | `plugins/marketplace/PluginListing.kt` |
| 3 | `plugins/marketplace/MarketplaceCache.kt` |
| 3 | `plugins/marketplace/MarketplaceClientImpl.kt` |
| 3 | `plugins/marketplace/UpdateNotifier.kt` |
| 3 | `plugins/marketplace/MarketplaceDataProvider.kt` |
| 3 | `commonTest/.../marketplace/MarketplaceIntegrationTest.kt` |

### Modified Files (4)
| File | Changes |
|------|---------|
| `UniversalPluginRegistry.kt` | Add endpoint storage, getEndpoint() |
| `UniversalPlugin.kt` | Add saveState(), restoreState() |
| `PluginHotReloader.kt` | Add full reload cycle |
| `RemotePluginDiscovery.kt` | Security integration, marketplace source |

---

## Dependencies Between Phases

```
Phase 1 (Hot Reload)
       ↓
Phase 2 (Security) ← requires reload for testing
       ↓
Phase 3 (Marketplace) ← requires security for distribution
```

---

**Created:** 2026-01-22
**Status:** Ready for Implementation
**Next:** `/i.implement` with `.swarm` for parallel execution
