# Feature Specification: Encrypted Permission Storage

**Feature Branch**: `001-encrypted-permission-storage`
**Created**: 2025-10-26
**Status**: Draft
**Input**: Implement encrypted permission storage for PluginSystem using hardware-backed encryption to protect plugin permission grants from unauthorized access via ADB, rooted devices, or backup exploitation.

## User Scenarios & Testing *(mandatory)*

<!--
  IMPORTANT: User stories should be PRIORITIZED as user journeys ordered by importance.
  Each user story/journey must be INDEPENDENTLY TESTABLE - meaning if you implement just ONE of them,
  you should still have a viable MVP (Minimum Viable Product) that delivers value.

  Assign priorities (P1, P2, P3, etc.) to each story, where P1 is the most critical.
  Think of each story as a standalone slice of functionality that can be:
  - Developed independently
  - Tested independently
  - Deployed independently
  - Demonstrated to users independently
-->

### User Story 1 - Hardware-Backed Permission Encryption (Priority: P1)

As a VOS4 security administrator, I need all plugin permission grants encrypted using hardware-backed encryption so that unauthorized users cannot read sensitive permission data even with ADB access or root privileges.

**Why this priority**: This is the core security feature that addresses the fundamental vulnerability. Without this, plugin permissions are stored in plain XML files readable by anyone with ADB access, exposing which plugins have been granted dangerous permissions (ACCESSIBILITY_SERVICES, CONTACTS, etc.). This is a critical privacy and security risk for users, especially those with disabilities who may be more vulnerable to exploitation.

**Independent Test**: Can be fully tested by:
1. Granting a plugin permission (e.g., ACCESSIBILITY_SERVICES to "com.example.plugin")
2. Using `adb shell cat /data/data/com.augmentalis.vos4/shared_prefs/*.xml` to verify permission data is encrypted
3. Attempting to read encryption keys via ADB (should fail - keys stored in hardware)
4. Verifying the app can still read and use the encrypted permissions correctly

This delivers immediate value by protecting all new permission grants from the moment it's deployed.

**Acceptance Scenarios**:

1. **Given** PluginSystem is initialized, **When** a plugin is granted ACCESSIBILITY_SERVICES permission, **Then** the permission grant is stored encrypted using hardware-backed AES256-GCM and cannot be read via ADB without device unlock
2. **Given** encrypted permissions exist, **When** PluginSystem queries granted permissions for a plugin, **Then** permissions are decrypted transparently and returned correctly within 5ms
3. **Given** device has hardware keystore (TEE/TrustZone), **When** encryption key is generated, **Then** key is stored in hardware keystore and cannot be extracted via software
4. **Given** encrypted permissions file exists, **When** malicious actor attempts to modify encrypted data, **Then** decryption fails with integrity check error and system logs security violation

---

### User Story 2 - Seamless Migration from Plain Storage (Priority: P2)

As a VOS4 user upgrading from a previous version, I need my existing plugin permissions automatically migrated to encrypted storage so that I don't lose any permission grants or have to re-grant permissions to all my plugins.

**Why this priority**: This ensures smooth upgrade path for existing users. Without migration, users would need to manually re-grant all permissions, creating friction and poor user experience. This is P2 because it only affects upgrade scenarios, not new installations.

**Independent Test**: Can be fully tested by:
1. Creating mock plain-text permission data in old SharedPreferences format
2. Launching PermissionStorage for the first time
3. Verifying all permissions are migrated to encrypted storage
4. Verifying old plain-text file is deleted after successful migration
5. Verifying no permissions are lost in migration

**Acceptance Scenarios**:

1. **Given** plain-text permission grants exist from previous version, **When** PermissionStorage initializes, **Then** all permissions are automatically migrated to encrypted storage and old file is deleted
2. **Given** migration is in progress, **When** app crashes mid-migration, **Then** migration resumes on next launch and completes successfully without data loss
3. **Given** migration completes successfully, **When** user queries plugin permissions, **Then** all previously granted permissions are intact and functional
4. **Given** migration encounters corrupted plain-text data, **When** PermissionStorage attempts migration, **Then** corrupted entries are logged and skipped while valid entries are migrated

---

### User Story 3 - Graceful Encryption Failure Handling (Priority: P3)

As a VOS4 user on an older device without hardware keystore, I need the system to fall back to software-based encryption so that plugin permissions remain functional even if hardware encryption is unavailable.

**Why this priority**: This handles edge case of devices without hardware keystore (pre-Android 6.0 or budget devices). While important for compatibility, it's P3 because VOS4's minimum API level is 28 (Android 9.0) which mandates hardware keystore, making this scenario rare. However, graceful degradation is still valuable for robustness.

**Independent Test**: Can be fully tested by:
1. Mocking hardware keystore failure (MasterKey.Builder throws exception)
2. Granting a plugin permission
3. Verifying system falls back to software-based encryption (StrongBox = false)
4. Verifying permissions remain functional with degraded security
5. Verifying user is notified of degraded security state

**Acceptance Scenarios**:

1. **Given** hardware keystore is unavailable, **When** PermissionStorage initializes, **Then** system falls back to software-based encryption and logs warning
2. **Given** software-based encryption is active, **When** plugin permission is granted, **Then** permission is encrypted using software keystore and remains functional
3. **Given** encryption fails completely, **When** permission storage is attempted, **Then** system logs critical error, notifies user, and denies permission grant (fail-secure)
4. **Given** device gains hardware keystore capability (OS upgrade), **When** PermissionStorage initializes, **Then** system migrates from software to hardware encryption on next permission modification

---

### User Story 4 - Backup and Restore Compatibility (Priority: P3)

As a VOS4 user performing device backup/restore, I need encrypted permissions to remain secure during backup so that cloud backups don't expose sensitive permission data, but also need permissions to be restored successfully on the same device.

**Why this priority**: This ensures security extends to backup scenarios. Without proper handling, encrypted data could be exposed in cloud backups or fail to restore. This is P3 because it's a secondary use case that doesn't affect normal operation, but is important for complete security coverage.

**Independent Test**: Can be fully tested by:
1. Granting several plugin permissions
2. Performing Android backup (adb backup or cloud backup)
3. Verifying backup file does not contain decryptable permission data
4. Restoring backup on same device
5. Verifying permissions are restored and functional
6. Attempting to restore on different device and verifying graceful failure

**Acceptance Scenarios**:

1. **Given** encrypted permissions exist, **When** device backup is created, **Then** backup contains encrypted data but NOT encryption keys (keys remain in Keystore only)
2. **Given** backup is restored on same device, **When** PermissionStorage initializes, **Then** encrypted permissions are decrypted successfully using device's Keystore
3. **Given** backup is restored on different device, **When** PermissionStorage attempts to decrypt, **Then** decryption fails gracefully and user is prompted to re-grant permissions
4. **Given** user disables backup for VOS4, **When** Android backup runs, **Then** permission data is excluded from backup entirely (android:allowBackup flag respects encryption sensitivity)

---

### Edge Cases

- **What happens when encryption key is deleted from Keystore?**
  - Scenario: User clears security credentials in Settings, or factory resets Keystore
  - Expected: PermissionStorage detects key loss, logs security event, generates new key, prompts user to re-grant all plugin permissions (cannot recover data without key)

- **What happens when device is rooted after encryption?**
  - Scenario: User roots device after permissions are encrypted
  - Expected: Hardware keystore remains protected (root cannot extract keys from TEE/TrustZone), encrypted data remains secure

- **What happens when SharedPreferences file is corrupted?**
  - Scenario: Disk corruption, power loss during write, or malicious file modification
  - Expected: PermissionStorage detects corruption via GCM authentication tag failure, logs error, deletes corrupted file, initializes fresh encrypted storage

- **What happens when concurrent permission grants occur?**
  - Scenario: Two plugins request permission simultaneously, or background sync conflicts with foreground grant
  - Expected: SharedPreferences write operations are atomic, last-write-wins with transaction safety

- **What happens when device is in Direct Boot mode (before first unlock)?**
  - Scenario: Device reboots, PluginSystem tries to access permissions before user unlocks device
  - Expected: Encrypted SharedPreferences requires device unlock to access Keystore, PermissionStorage returns empty permissions or cached data until unlock

- **What happens when API 23-27 device tries to use this feature?**
  - Scenario: User installs VOS4 on Android 6.0-8.1 (below minimum API 28)
  - Expected: App refuses to install (minSdk=28 in manifest), but if somehow installed, EncryptedSharedPreferences library handles graceful degradation

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST encrypt all plugin permission grants using AES256-GCM encryption scheme with hardware-backed key storage
- **FR-002**: System MUST store encryption keys exclusively in Android Keystore (hardware TEE/TrustZone when available) and NEVER in software-accessible storage
- **FR-003**: System MUST use EncryptedSharedPreferences with PrefKeyEncryptionScheme.AES256_SIV for key encryption and PrefValueEncryptionScheme.AES256_GCM for value encryption
- **FR-004**: System MUST maintain backward compatibility by automatically migrating plain-text permission grants from previous versions to encrypted storage on first launch
- **FR-005**: System MUST delete original plain-text permission files immediately after successful migration to prevent data leakage
- **FR-006**: System MUST handle encryption failures gracefully by logging errors, notifying user, and failing secure (deny permission grant rather than store unencrypted)
- **FR-007**: System MUST validate encryption integrity on every read operation using GCM authentication tags and reject corrupted data
- **FR-008**: System MUST add less than 5ms latency to permission grant/query operations compared to plain-text storage
- **FR-009**: System MUST log all encryption failures, key generation events, and migration activities to security audit log
- **FR-010**: System MUST exclude encryption keys from Android backup mechanism (keys remain device-bound)
- **FR-011**: System MUST support permission query operations during Direct Boot mode by caching permissions or returning safe defaults until device unlock
- **FR-012**: System MUST generate new encryption key if existing key is lost or corrupted, and prompt user to re-grant all permissions
- **FR-013**: System MUST use StrongBox Keymaster when available (Android 9+) for enhanced hardware security

### Non-Functional Requirements

- **NFR-001**: Encryption/decryption operations MUST NOT block UI thread (perform on background thread or coroutine)
- **NFR-002**: System MUST be compatible with Android API 28+ (matching VOS4 minimum SDK)
- **NFR-003**: Encrypted storage MUST NOT increase memory footprint by more than 2MB compared to plain-text storage
- **NFR-004**: Implementation MUST use AndroidX Security library (androidx.security:security-crypto) for standardized encryption patterns
- **NFR-005**: Code MUST be fully documented with KDoc explaining security implications of each method
- **NFR-006**: Implementation MUST include unit tests achieving >90% code coverage for encryption/decryption paths
- **NFR-007**: Migration logic MUST be idempotent (safe to run multiple times without data loss)

### Key Entities *(include if feature involves data)*

- **Permission Grant**: Represents a single permission granted to a plugin
  - Plugin ID (String): Reverse-domain plugin identifier (e.g., "com.augmentalis.speech-engine")
  - Permission Type (Permission enum): Type of permission (ACCESSIBILITY_SERVICES, CONTACTS, MICROPHONE, etc.)
  - Grant Status (GrantStatus enum): Current status (GRANTED, DENIED, PENDING, REVOKED)
  - Grant Timestamp (Long): Unix timestamp when permission was granted
  - Revocation Timestamp (Long?): Optional timestamp when permission was revoked
  - Granted By (String?): Optional user ID or system identifier that granted permission
  - Storage Format: Encrypted string in SharedPreferences with key format `{pluginId}.{permission}` and value format `{status}|{timestamp}|{grantedBy}`

- **Encryption Key**: Cryptographic key for encrypting permission data
  - Key Alias (String): Identifier in Android Keystore (`_plugin_permissions_master_key_`)
  - Key Scheme (MasterKey.KeyScheme): AES256_GCM (256-bit AES in GCM mode)
  - Key Storage: Android Keystore (hardware TEE/TrustZone when available, software keystore as fallback)
  - Key Lifespan: Permanent (device-bound, cannot be exported)
  - Key Access: Requires device unlock (UserAuthentication not required for background access)

- **Migration State**: Tracks migration status from plain-text to encrypted storage
  - Migration Completed (Boolean): Whether migration has been performed
  - Migrated Count (Int): Number of permissions migrated
  - Failed Count (Int): Number of permissions that failed to migrate
  - Migration Timestamp (Long): When migration completed
  - Storage: Separate encrypted SharedPreferences key `_migration_state_` to prevent re-migration

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Permission grants are unreadable via `adb shell` without device unlock, verified by attempting to read SharedPreferences XML file and confirming data is encrypted (not plain JSON/text)
- **SC-002**: Encryption adds less than 5ms latency to permission operations, verified by benchmark tests comparing encrypted vs plain-text storage for 100 permission operations (grant, query, revoke)
- **SC-003**: All existing permissions migrate successfully on first app launch, verified by automated test that creates 50 mock plain-text permissions, runs migration, and confirms 100% success rate
- **SC-004**: Encryption keys are stored in hardware Keystore (not software-accessible storage), verified by checking MasterKey.isInsideSecureHardware() returns true on devices with TEE/TrustZone
- **SC-005**: Corrupted encrypted data is detected and rejected, verified by manually corrupting SharedPreferences XML, attempting to read, and confirming GCM authentication failure
- **SC-006**: Zero plain-text permission data remains after migration, verified by searching all SharedPreferences XML files for known permission patterns and finding zero matches
- **SC-007**: System logs all security events (key generation, migration, encryption failures), verified by triggering each scenario and confirming corresponding log entries in logcat with tag "PluginSecurity"
- **SC-008**: Permission storage survives app restart and device reboot, verified by granting permissions, killing app, rebooting device, and confirming permissions remain intact
- **SC-009**: Concurrent permission grants do not corrupt data, verified by stress test granting 100 permissions simultaneously from multiple threads and confirming all are stored correctly
- **SC-010**: Backup/restore does not leak unencrypted permission data, verified by performing Android backup, extracting backup file, and confirming no plain-text permission data exists

### Business Value

- **BV-001**: Reduces security vulnerability surface area by eliminating plain-text storage of sensitive permission grants
- **BV-002**: Enables VOS4 to meet data protection compliance requirements (GDPR, CCPA, HIPAA for medical plugins) by ensuring permission data is encrypted at rest
- **BV-003**: Increases user trust in VOS4 platform by demonstrating commitment to security best practices
- **BV-004**: Reduces support burden by preventing permission-related security incidents that would require investigation and remediation
- **BV-005**: Enables third-party plugin ecosystem growth by providing secure permission management that developers can trust

### Technical Debt Prevention

- **TD-001**: Use industry-standard AndroidX Security library to avoid custom encryption implementation errors
- **TD-002**: Design migration logic to be idempotent and version-aware, enabling future encryption upgrades (e.g., AES256 → AES512) without breaking changes
- **TD-003**: Abstract encryption behind PermissionStorage interface to enable future storage backends (e.g., Room database) without changing consumers
- **TD-004**: Document all security assumptions and limitations in KDoc to prevent future developers from weakening security
- **TD-005**: Include comprehensive tests to prevent regression if future changes modify encryption logic
