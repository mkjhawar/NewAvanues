# VoiceOSCore-Analysis-VOSSyncE2ETestPlan-260211-V1

## Summary

Comprehensive end-to-end test plan for VOS SFTP Sync (Phase B). Covers Docker SFTP test server setup, prerequisites, 12 core test cases, 5 edge cases, and verification procedures for the complete VOS synchronization pipeline.

**Phase B Goal**: Enable users to sync .web.vos files from cloud SFTP server, download new scraped commands for unscraped websites, and maintain version/hash tracking across devices.

---

## 1. Test Server Setup

### 1.1 Docker SFTP Container (Recommended)

#### Quick Start (5 minutes)

```bash
# Run SFTP server in Docker
docker run -d \
  --name sftp-vos-test \
  -p 2222:22 \
  -v ~/vos-sftp:/home/user/vos \
  atmoz/sftp \
  user:pass:1001

# Verify container is running
docker ps | grep sftp-vos-test

# Create VOS directory on server
docker exec sftp-vos-test mkdir -p /home/user/vos

# Verify permissions
docker exec sftp-vos-test ls -la /home/user/vos
```

#### Container Details

| Parameter | Value | Purpose |
|-----------|-------|---------|
| Name | `sftp-vos-test` | Container identifier |
| Port | `2222:22` | Map host:2222 → container:22 (SSH) |
| Volume | `~/vos-sftp:/home/user/vos` | Persistent storage on host |
| User | `user` | SFTP login username |
| Password | `pass` | SFTP login password |
| UID | `1001` | Linux user ID for file ownership |
| Base Image | `atmoz/sftp` | Alpine-based, minimal footprint |

#### Dockerfile Alternative (if Docker Compose preferred)

```yaml
version: '3.8'
services:
  sftp:
    image: atmoz/sftp:latest
    container_name: sftp-vos-test
    ports:
      - "2222:22"
    environment:
      - SFTP_USERS=user:pass:1001
    volumes:
      - ~/vos-sftp:/home/user/vos
    restart: unless-stopped
```

Deploy with:
```bash
docker-compose -f docker-compose.yml up -d
```

### 1.2 Manual SFTP Server Setup (Alternative)

If Docker is unavailable:

**Linux/macOS with OpenSSH**:
```bash
# Install OpenSSH (if needed)
# macOS: brew install openssh
# Ubuntu: sudo apt install openssh-server

# Start SSH server
sudo systemctl start ssh

# Create dedicated SFTP user
sudo useradd -m -s /bin/bash sftp-user
sudo passwd sftp-user

# Create VOS directory
sudo mkdir -p /var/sftp/vos
sudo chown sftp-user:sftp-user /var/sftp/vos
sudo chmod 755 /var/sftp/vos

# Restrict SSH access (optional but recommended)
sudo usermod -s /usr/sbin/nologin sftp-user
```

**SSH Key Authentication Setup**:
```bash
# Generate test key pair
ssh-keygen -t rsa -b 2048 -f ~/.ssh/vos-sftp-test -N ""

# Copy public key to server
ssh-copy-id -i ~/.ssh/vos-sftp-test.pub sftp-user@localhost

# Verify key auth works
sftp -i ~/.ssh/vos-sftp-test sftp-user@localhost
```

### 1.3 Connection Verification

Test SFTP connectivity before running test cases:

```bash
# Test password auth
sftp -oPort=2222 user@localhost

# Commands to verify server state
ls -la /vos
pwd
exit

# Test SSH key auth (if configured)
sftp -i ~/.ssh/vos-sftp-test -oPort=2222 sftp-user@localhost
```

**Expected Output**:
```
Connected to user@localhost.
sftp> ls -la
drwxr-xr-x    2 1001     1001         4096 Feb 11 08:00 vos
sftp> exit
```

---

## 2. Prerequisites

Before executing test cases, verify all prerequisites are met.

### 2.1 Device/Emulator Setup

| Requirement | Status | Notes |
|-------------|--------|-------|
| Android SDK ≥ 30 | ✓ Required | VoiceOS target API |
| Gradle build succeeds | ✓ Required | `./gradlew assembleDebug` passes |
| VoiceOS app installed | ✓ Required | On real device or emulator |
| Avanues app launched | ✓ Required | Home screen visible |
| Accessibility service enabled | ✓ Required | Settings → Accessibility → VoiceOS |
| Voice recognition ready | ✓ Required | Test with "hello" command |

**Verification Steps**:
```bash
# Check if app is installed
adb shell pm list packages | grep avanues

# Check if accessibility service is running
adb shell dumpsys accessibility | grep VoiceOS

# Verify home screen active
adb shell am stack list | grep com.augmentalis.avanues
```

### 2.2 VOS Sync Settings Configuration

Navigate to: **Settings → Voice Control → VOS Sync** (new settings page)

| Setting | Expected Value | Notes |
|---------|---|---|
| **SFTP Enabled** | ON/OFF toggle | Master enable/disable for sync |
| **SFTP Host** | `192.168.1.100` or hostname | Server IP/hostname (local network IP for Docker) |
| **SFTP Port** | `2222` | Match Docker container port mapping |
| **SFTP Username** | `user` | Docker container default user |
| **SFTP Password** | `pass` | Docker container default password (or SSH key) |
| **Auth Type** | Password / SSH Key | Test both variants |
| **SSH Key Path** (if key auth) | `/storage/emulated/0/vos-sftp-test` | Path to private key on device |
| **Remote Base Path** | `/vos` | Directory on SFTP server containing manifest + .web.vos files |
| **Local Cache Dir** | `/data/data/com.augmentalis.avanues/vos-cache/` | Where downloaded .web.vos files are stored |
| **Auto Sync Enabled** | ON | Background sync via WorkManager (optional for Phase B) |
| **Sync Interval (hours)** | `24` | Daily sync frequency |

### 2.3 VOS Sync Screen Visibility

New UI component: **VosSyncScreen** (composable in `AvanueUI/components/settings/`)

**Expected visual state**:
- Status dot (green=connected, red=error, gray=unconfigured, yellow=syncing)
- Connection status text ("Connected", "Disconnected", "Syncing", "Error: {message}")
- Progress bar (0-100%) during active sync
- Action message area (success/error toast)
- "Test Connection" button
- "Upload Files" button (for .app.vos only)
- "Download Files" button (for .web.vos)
- "Full Sync" button (both directions)
- File list section with registry data

### 2.4 Database & File System State

**Before Test Suite**:

1. **Clear VOS file registry** (optional):
   ```bash
   adb shell su -c "rm /data/data/com.augmentalis.avanues/databases/avanues.db"
   ```
   (This resets registry to empty; seed .app.vos files are re-loaded on app start)

2. **Clear local VOS cache**:
   ```bash
   adb shell rm -rf /data/data/com.augmentalis.avanues/vos-cache/
   ```

3. **Verify SFTP server VOS directory is empty or contains test files**:
   ```bash
   docker exec sftp-vos-test ls -la /home/user/vos/
   ```

### 2.5 Logcat Filters

Enable logcat monitoring for VOS-related operations:

```bash
# In separate terminal, run:
adb logcat -s "VosSyncManager:I VosSftpClient:I CommandManager:I" --line-buffered

# Or, save to file:
adb logcat -s "VosSyncManager:I VosSftpClient:I CommandManager:I" > /tmp/vos-sync.log &
```

**Key log tags**:
- `VosSyncManager` — Overall sync orchestration
- `VosSftpClient` — SFTP protocol operations
- `CommandManager` — VOS loading, registry updates
- `VosFileRegistry` — Database operations

---

## 3. Test Matrix

### 3.1 Test Case Definitions

All test cases assume:
- SFTP server is running (Docker or manual)
- App is installed and Accessibility service is enabled
- VosSyncScreen is visible in Settings

| Test Case | Steps | Expected Result | Status | Notes |
|-----------|-------|---|---|---|
| **TC-01: Connection Success** | 1. Configure SFTP settings (host=localhost:2222, user=user, pass=pass)<br/>2. Tap "Test Connection" button<br/>3. Observe connection status | ✓ Status dot turns GREEN<br/>✓ Text shows "Connected"<br/>✓ No error message<br/>✓ Logcat shows "SFTP connection successful" | PENDING | Baseline test; all others depend on this passing |
| **TC-02: Connection Failure (Wrong Host)** | 1. Set SFTP Host to invalid IP (e.g., `192.168.1.255`)<br/>2. Tap "Test Connection"<br/>3. Wait 5 seconds for timeout | ✓ Status dot turns RED<br/>✓ Text shows "Connection Failed"<br/>✓ Error message: "Host unreachable" or "Connection timeout"<br/>✓ Logcat shows exception trace | PENDING | Validates timeout handling; should not hang app |
| **TC-03: Connection Failure (Wrong Credentials)** | 1. Set SFTP password to wrong value (e.g., `wrongpass`)<br/>2. Tap "Test Connection"<br/>3. Observe result | ✓ Status dot turns RED<br/>✓ Text shows "Auth Failed"<br/>✓ Error message: "Authentication failed"<br/>✓ Logcat shows "SSH_MSG_USERAUTH_FAILURE" | PENDING | Verifies credential validation |
| **TC-04: Upload Local VOS Files** | 1. Ensure TC-01 passes (connection OK)<br/>2. Tap "Upload Files" button<br/>3. Select app commands from local registry<br/>4. Wait for upload to complete<br/>5. Check server-side VOS directory | ✓ Progress bar appears, animates to 100%<br/>✓ Upload message: "Uploaded 2 files (en-US.app.vos, es-ES.app.vos)"<br/>✓ Logcat shows "SFTP: PUT /home/user/vos/en-US.app.vos (45KB)"<br/>✓ Files appear on server: `sftp> ls -la /vos/` | PENDING | Tests local-to-remote transfer |
| **TC-05: Download New VOS Files from Server** | 1. Manually copy test .web.vos files to server:<br/>   `scp -P 2222 en-US.web.vos user@localhost:/home/user/vos/`<br/>2. In app, tap "Download Files" button<br/>3. Select web commands to download<br/>4. Wait for download | ✓ Progress bar animates (0→100%)<br/>✓ Download message: "Downloaded 1 file (en-US.web.vos, 22KB)"<br/>✓ File appears in cache: `/data/data/.../vos-cache/en-US.web.vos`<br/>✓ Logcat shows "SFTP: GET /home/user/vos/en-US.web.vos"<br/>✓ VoiceCommandEntity count increases in DB | PENDING | Tests remote-to-local transfer |
| **TC-06: Full Sync (Upload + Download)** | 1. Ensure server has test .web.vos files<br/>2. Tap "Full Sync" button<br/>3. Wait 10-15 seconds for completion<br/>4. Verify both upload + download actions occurred | ✓ Progress bar shows upload phase (0-50%), then download phase (50-100%)<br/>✓ Message: "Sync complete: Uploaded 2 files, Downloaded 1 file"<br/>✓ Files visible on server<br/>✓ Local cache populated with .web.vos<br/>✓ Registry updated with timestamps | PENDING | Integration test of full cycle |
| **TC-07: Manifest Creation/Update on Server** | 1. After TC-04 or TC-06, check server for manifest.json<br/>2. SSH into server and inspect manifest content | ✓ File `/home/user/vos/manifest.json` exists<br/>✓ Contains entries for each uploaded file:<br/>```json<br/>{<br/>  "version": "2.1",<br/>  "generatedAt": "2026-02-11T10:30:00Z",<br/>  "files": [<br/>    {"name": "en-US.app.vos", "hash": "abc123...", "size": 45000, "domain": "app"}<br/>  ]<br/>}<br/>```<br/>✓ Hash matches file content (SHA-256) | PENDING | Validates manifest versioning |
| **TC-08: Delta Sync (No Re-download of Existing Files)** | 1. Run TC-05 (download en-US.web.vos)<br/>2. Record file hash from registry<br/>3. Wait 30 seconds<br/>4. Tap "Download Files" again<br/>5. Verify file is not re-downloaded | ✓ Second "Download Files" completes instantly<br/>✓ Logcat shows "en-US.web.vos: hash match, skipping"<br/>✓ File modification timestamp unchanged<br/>✓ Network traffic minimal (manifest read only, no file transfer) | PENDING | Validates delta logic; saves bandwidth |
| **TC-09: SSH Key Authentication** | 1. Configure SSH key auth on SFTP server (if available)<br/>2. In app, set Auth Type to "SSH Key"<br/>3. Provide path to private key on device<br/>4. Tap "Test Connection"<br/>5. Verify connection succeeds | ✓ Status dot turns GREEN<br/>✓ Text shows "Connected"<br/>✓ No password prompt<br/>✓ Logcat shows "SSH key auth successful"<br/>✓ Can upload/download without password | PENDING | Tests alternative auth method |
| **TC-10: Network Interruption During Sync** | 1. Start full sync (TC-06)<br/>2. After 5 seconds (mid-transfer), disconnect network:<br/>   - Disable WiFi, or<br/>   - `adb shell cmd connectivity airplane-mode enable`<br/>3. Observe behavior for 10 seconds<br/>4. Re-enable network<br/>5. Check final state | ✓ Progress bar freezes<br/>✓ Timeout message after 30 seconds: "Network error: connection reset"<br/>✓ Status dot turns YELLOW (retrying)<br/>✓ Sync resumes after network restored<br/>✓ No partial files left on server<br/>✓ No partial files in cache (or marked incomplete) | PENDING | Critical resilience test |
| **TC-11: Large File Handling (>1MB VOS File)** | 1. Create synthetic large .web.vos file (1.5MB) on server<br/>2. Tap "Download Files"<br/>3. Monitor progress and memory usage<br/>4. Verify file integrity after transfer | ✓ Progress bar animates smoothly (not frozen)<br/>✓ File downloaded completely (1.5MB)<br/>✓ SHA-256 hash matches server file<br/>✓ App does not crash or show OOM warnings<br/>✓ Logcat shows chunked transfer (e.g., "downloaded 256KB, 512KB, 1024KB, 1536KB")<br/>✓ Memory usage stays <50MB during transfer | PENDING | Handles real-world file sizes |
| **TC-12: Empty Server (No Manifest)** | 1. Clear server VOS directory: `docker exec sftp-vos-test rm -rf /home/user/vos/*`<br/>2. Tap "Download Files"<br/>3. Observe behavior | ✓ No error; graceful degradation<br/>✓ Message: "No files available on server"<br/>✓ Status dot remains GREEN (server is reachable)<br/>✓ Local cache unchanged<br/>✓ Logcat shows "manifest.json not found; treating as empty repository" | PENDING | Edge case: new/empty server |

---

## 4. Edge Cases

These tests validate error handling and boundary conditions.

### 4.1 Server Unreachable Mid-Transfer (Extended)

**Scenario**: SFTP connection established, but server crashes during file transfer.

**Test Procedure**:
1. Start download of 1MB file (TC-11)
2. After 200KB transferred, kill SFTP server:
   ```bash
   docker kill sftp-vos-test
   ```
3. Wait 30 seconds for timeout
4. Restart server:
   ```bash
   docker start sftp-vos-test
   ```
5. Manually retry sync

**Expected Results**:
- Partial file NOT written to cache (or marked with `.tmp` extension)
- Connection timeout error shown in 30 seconds (not hung)
- Retry succeeds after server restart
- Final file is complete and valid

### 4.2 Manifest Corrupted JSON

**Scenario**: Server manifest.json contains invalid JSON (syntax error).

**Test Procedure**:
1. SSH into server:
   ```bash
   docker exec -it sftp-vos-test bash
   echo '{invalid json}' > /home/user/vos/manifest.json
   exit
   ```
2. In app, tap "Download Files"

**Expected Results**:
- JSON parse exception caught
- Error message: "Manifest format error: expected '{' at line 1"
- Status dot turns RED
- App continues to run (no crash)
- Logcat shows exception with line number

### 4.3 VOS File with Invalid Format

**Scenario**: Downloaded .web.vos file does not conform to VOS v2.1 spec.

**Test Procedure**:
1. Create invalid .web.vos file:
   ```bash
   echo '"not_an_object"' > /home/user/vos/en-US.web.vos
   docker cp en-US.web.vos sftp-vos-test:/home/user/vos/
   ```
2. In app, tap "Download Files" with this file selected
3. Observe import behavior

**Expected Results**:
- File downloaded successfully (SFTP level OK)
- Import fails with schema validation error
- Message: "Invalid VOS file format: missing 'commands' array"
- File NOT inserted into database
- Cache file preserved for manual inspection
- App continues to run

### 4.4 Concurrent Sync Attempts

**Scenario**: User taps "Full Sync" twice in rapid succession (e.g., double-tap).

**Test Procedure**:
1. In VosSyncScreen, rapidly tap "Full Sync" button twice within 1 second
2. Observe state changes

**Expected Results**:
- First sync starts immediately
- Second tap is ignored or shows toast: "Sync already in progress"
- Only one SFTP connection established
- Single manifest.json created (no duplicate writes)
- No race condition or data corruption

### 4.5 Disk Full on Download

**Scenario**: Device storage is full when attempting to download large .web.vos file.

**Test Procedure**:
1. Fill device storage to ~99% (e.g., via large test files in DCIM)
2. In app, tap "Download Files" for 500KB file
3. Observe behavior

**Expected Results**:
- Download begins but fails when writing to cache
- Error message: "Insufficient storage: 50MB required, 2MB available"
- Partial file removed from cache (cleanup)
- SFTP connection closed gracefully
- Status dot remains reachable but download incomplete
- User is directed to free up space

---

## 5. Verification Steps

### 5.1 VosSyncScreen UI Verification

**Connection Status Dot**:
```
Color State Reference:
- GREEN:  Connected and idle
- YELLOW: Syncing in progress or retrying
- RED:    Error or disconnected
- GRAY:   Not configured (empty SFTP host field)
```

**Verification**:
1. In Settings, navigate to Voice Control → VOS Sync
2. Observe status dot color after each action (Test Connection, Upload, Download)
3. Compare against expected color states in test cases

### 5.2 Progress Bar Verification

**Expected Behavior**:
- Starts at 0% when action begins
- Animates smoothly (not jumpy) in 5-10% increments
- Reaches 100% when action completes
- Disappears after completion or error

**Verification Steps**:
```
For upload (TC-04):
1. Tap "Upload Files"
2. Count: progress bar should move every 100-200ms
3. At 100%, bar disappears and message shows

For download (TC-05):
1. Tap "Download Files"
2. Progress bar fills over ~5-10 seconds (for typical .web.vos size)
3. Completion message appears
```

### 5.3 Action Message Verification

**Message Format**:
```
Success:  "✓ Downloaded 1 file (en-US.web.vos, 22KB)"
Error:    "✗ Connection failed: Host unreachable"
Warning:  "⚠ 1 file skipped (hash match)"
Info:     "Syncing..."
```

**Verification**:
1. After each operation, check message text below progress bar
2. Verify emoji icon matches outcome (✓, ✗, ⚠)
3. Confirm file count and sizes are accurate

### 5.4 File List Registry Section

**UI Component**: Expandable section in VosSyncScreen showing downloaded files.

**Verification Steps**:
1. Tap "Sync History" or "File Registry" section
2. Observe list of VOS files:
   ```
   en-US.app.vos (62 commands)
     Domain: app | Locale: en-US
     Size: 45KB | Hash: abc123...7f
     Synced: 2026-02-11 10:30:42

   en-US.web.vos (45 commands)
     Domain: web | Locale: en-US
     Size: 22KB | Hash: def456...8g
     Synced: 2026-02-11 10:35:15
   ```
3. Verify each entry shows:
   - File name and command count
   - Domain (app/web)
   - Locale (en-US, es-ES, etc.)
   - File size
   - Content hash (first 8 + last 8 characters)
   - Timestamp (synced_at from database)

### 5.5 Database Verification

**VoiceCommandEntity Count**:
```bash
adb shell su -c \
  "sqlite3 /data/data/com.augmentalis.avanues/databases/avanues.db \
  'SELECT COUNT(*) FROM voice_command WHERE locale=\"en-US\";'"
```

**Expected Output**:
```
Before download:  62 (app commands only)
After TC-05:      107 (62 app + 45 web commands)
```

**VosFileRegistry Verification**:
```bash
adb shell su -c \
  "sqlite3 /data/data/com.augmentalis.avanues/databases/avanues.db \
  'SELECT file_name, domain, command_count, synced_at FROM vos_file_registry;'"
```

**Expected Output**:
```
en-US.app.vos|app|62|2026-02-11 10:30:42
en-US.web.vos|web|45|2026-02-11 10:35:15
```

### 5.6 Timestamp Verification

**uploaded_at Field** (for uploaded files):
```bash
adb shell su -c \
  "sqlite3 /data/data/com.augmentalis.avanues/databases/avanues.db \
  'SELECT file_name, synced_at FROM vos_file_registry WHERE source=\"cloud\";'"
```

**Verification**:
- Timestamp is ISO 8601 format: `2026-02-11T10:35:15Z`
- Matches server-side generation time (within 1 second)
- Updates on each successful sync

### 5.7 Logcat Verification

**Capture logs for each test case**:
```bash
# Start fresh logcat
adb logcat --clear

# Run test case (e.g., TC-01)
# Tap "Test Connection"

# Capture output
adb logcat -d -s "VosSyncManager VosSftpClient CommandManager" > /tmp/tc-01.log

# Review log
cat /tmp/tc-01.log
```

**Expected Log Pattern for TC-01 (Connection Success)**:
```
I/VosSyncManager: [VosSyncManager] testConnection() started
I/VosSftpClient:  [VosSftpClient] Connecting to localhost:2222
I/VosSftpClient:  [VosSftpClient] SSH authentication: user@localhost
I/VosSftpClient:  [VosSftpClient] SFTP session established
I/VosSyncManager: [VosSyncManager] testConnection() succeeded in 234ms
```

**Expected Log Pattern for TC-02 (Wrong Host)**:
```
I/VosSyncManager: [VosSyncManager] testConnection() started
I/VosSftpClient:  [VosSftpClient] Connecting to 192.168.1.255:2222
E/VosSftpClient:  [VosSftpClient] Connection timeout after 30000ms
E/VosSyncManager: [VosSyncManager] testConnection() failed: java.net.SocketTimeoutException
```

**Expected Log Pattern for TC-04 (Upload)**:
```
I/VosSyncManager: [VosSyncManager] uploadLocalFiles() started
I/VosSftpClient:  [VosSftpClient] Uploading en-US.app.vos
I/VosSftpClient:  [VosSftpClient] PUT /home/user/vos/en-US.app.vos (45000 bytes)
I/VosSftpClient:  [VosSftpClient] Upload complete: en-US.app.vos
I/VosSyncManager: [VosSyncManager] uploadLocalFiles() completed: 2 files, 89KB
```

**Expected Log Pattern for TC-05 (Download)**:
```
I/VosSyncManager: [VosSyncManager] downloadRemoteFiles() started
I/VosSftpClient:  [VosSftpClient] Downloading en-US.web.vos
I/VosSftpClient:  [VosSftpClient] GET /home/user/vos/en-US.web.vos (22000 bytes)
I/VosSftpClient:  [VosSftpClient] Downloaded 22000 bytes to cache
I/CommandManager: [CommandManager] Importing en-US.web.vos: 45 commands
I/VosSyncManager: [VosSyncManager] downloadRemoteFiles() completed: 1 file, 22KB
```

---

## 6. Test Execution Workflow

### 6.1 Pre-Test Checklist

- [ ] SFTP server running (Docker or manual)
- [ ] Device/emulator connected via USB/ADB
- [ ] VoiceOS app installed and accessibility service enabled
- [ ] VosSyncScreen visible in Settings
- [ ] Logcat terminal open with filters
- [ ] Test SFTP credentials verified (TC-01 prerequisite)
- [ ] Server VOS directory confirmed to exist
- [ ] Database cleared (optional; recommended for clean test)

### 6.2 Test Execution Order

**Recommended execution order** (dependencies shown):

```
TC-01 (Connection Success) ← MUST PASS (gate for all others)
  ├── TC-02 (Wrong Host)
  ├── TC-03 (Wrong Credentials)
  ├── TC-04 (Upload) ← required for TC-07
  │   └── TC-07 (Manifest)
  ├── TC-05 (Download) ← required for TC-08
  │   └── TC-08 (Delta Sync)
  ├── TC-06 (Full Sync) ← requires TC-04 + TC-05 logic
  ├── TC-09 (SSH Key Auth) ← separate path (if supported)
  ├── TC-10 (Network Interruption)
  ├── TC-11 (Large File)
  └── TC-12 (Empty Server)

Edge Cases (parallel execution; order independent):
  ├── EC-01 (Server Unreachable Mid-Transfer)
  ├── EC-02 (Manifest Corrupted)
  ├── EC-03 (Invalid VOS File)
  ├── EC-04 (Concurrent Sync)
  └── EC-05 (Disk Full)
```

### 6.3 Pass/Fail Criteria

**Test Case Status**:
- **PASS**: All expected results verified, no errors in logcat
- **FAIL**: Any expected result missing or error encountered
- **SKIP**: Blocked by prerequisite (e.g., TC-05 skipped if TC-01 failed)
- **PARTIAL**: Some sub-steps passed; specific failures noted

**Overall Test Suite Result**:
- **PASS**: TC-01, TC-04, TC-05, TC-06 all pass + at least 3 other TC's pass
- **WARN**: 2+ test cases fail but not blocking (e.g., SSH key auth optional)
- **FAIL**: TC-01 fails or 4+ test cases fail

---

## 7. Configuration Reference

### 7.1 VOS Sync Settings Schema (DataStore)

```kotlin
data class VosSyncConfig(
  val sftpEnabled: Boolean,          // Master toggle
  val sftpHost: String,              // hostname or IP
  val sftpPort: Int,                 // default 2222 for Docker, 22 for standard SSH
  val sftpUsername: String,          // "user" for Docker
  val sftpPassword: String,          // "pass" for Docker (encrypted in DataStore)
  val authType: AuthType,            // PASSWORD or SSH_KEY
  val sshKeyPath: String?,           // path to private key (if SSH_KEY)
  val remoteBasePath: String,        // "/vos"
  val localCacheDir: String,         // app cache directory
  val autoSyncEnabled: Boolean,      // WorkManager periodic task
  val syncIntervalHours: Int,        // 24 (default)
  val lastSyncTime: Long?            // epoch millis
)

enum class AuthType {
  PASSWORD,
  SSH_KEY
}
```

### 7.2 SFTP Server Configuration

**Docker Environment**:
```
Host: localhost (from emulator use 10.0.2.2 or host gateway IP)
Port: 2222
User: user
Password: pass
Base Path: /home/user/vos
```

**Manual SSH Server**:
```
Host: localhost or 127.0.0.1
Port: 22 (standard) or custom
User: sftp-user
Password: (set via passwd)
Base Path: /var/sftp/vos or custom
```

### 7.3 Manifest.json Schema

**File Location**: `/home/user/vos/manifest.json` (on SFTP server)

**Schema v2.1**:
```json
{
  "version": "2.1",
  "generatedAt": "2026-02-11T10:30:00Z",
  "appVersion": "1.0.0",
  "files": [
    {
      "name": "en-US.app.vos",
      "domain": "app",
      "locale": "en-US",
      "size": 45000,
      "hash": "abc123def456ghi789jkl012mno345pqr678stu901vwx234yz",
      "generatedAt": "2026-02-11T10:00:00Z",
      "commandCount": 62
    },
    {
      "name": "en-US.web.vos",
      "domain": "web",
      "locale": "en-US",
      "size": 22000,
      "hash": "def456ghi789jkl012mno345pqr678stu901vwx234yz567abc",
      "generatedAt": "2026-02-11T09:30:00Z",
      "commandCount": 45
    }
  ]
}
```

---

## 8. Troubleshooting Guide

### 8.1 Docker SFTP Container Issues

**Problem**: Container fails to start
```bash
# Check logs
docker logs sftp-vos-test

# Verify port not in use
lsof -i :2222

# Restart container
docker restart sftp-vos-test
```

**Problem**: Cannot connect from app
- If using emulator: use `10.0.2.2` as host (not `localhost`)
- If using physical device on same network: use device host IP (e.g., `192.168.1.100`)
- Check firewall: `sudo ufw allow 2222`

### 8.2 SFTP Connection Errors

**"Host unreachable"**:
- Verify SFTP server is running: `docker ps | grep sftp-vos-test`
- Verify port is correct: `adb shell nc -zv localhost 2222`
- Check network routing from device to host

**"Authentication failed"**:
- Verify credentials in settings match Docker defaults (`user:pass`)
- Check SFTP user exists: `docker exec sftp-vos-test id user`

**"Permission denied"**:
- Verify VOS directory permissions: `docker exec sftp-vos-test ls -la /home/user/vos/`
- Fix if needed: `docker exec sftp-vos-test chmod 755 /home/user/vos`

### 8.3 Sync Operation Failures

**Upload fails**:
- Check remote directory writable: `docker exec sftp-vos-test touch /home/user/vos/test.txt`
- Verify local .app.vos files exist in app cache
- Check disk space on server: `docker exec sftp-vos-test df -h`

**Download fails**:
- Verify remote .web.vos file exists: `docker exec sftp-vos-test ls /home/user/vos/`
- Check local cache directory writable: `adb shell ls -la /data/data/com.augmentalis.avanues/`
- Check device storage space: `adb shell df /data/data/`

**Manifest not found**:
- Create empty manifest if not exists:
  ```bash
  docker exec sftp-vos-test bash -c 'echo "{\"version\":\"2.1\",\"files\":[]}" > /home/user/vos/manifest.json'
  ```

### 8.4 UI Verification Issues

**VosSyncScreen not visible**:
- Verify Settings app is compiled with UI module
- Check AvanueUI DesignSystem imported in build.gradle.kts
- Restart app: `adb shell am force-stop com.augmentalis.avanues && adb shell am start -n com.augmentalis.avanues/.MainActivity`

**Progress bar not animating**:
- Check if sync operation is actually running: inspect logcat
- Verify UI framework thread (Compose) is not blocked
- Check device performance settings (animation scale not 0)

### 8.5 Database Issues

**VosFileRegistry table not created**:
- Rebuild app: `./gradlew clean assembleDebug`
- SQLDelight schema migration runs on app first launch
- Verify database schema: `adb shell sqlite3 /data/data/.../avanues.db ".schema vos_file_registry"`

**Voice commands not loading**:
- Check CommandLoader extensions are wired: grep `loadWebCommands` in codebase
- Verify import succeeds: check logcat for "Importing X commands"
- Inspect DB directly: `adb shell sqlite3 ... "SELECT COUNT(*) FROM voice_command;"`

---

## 9. Test Report Template

Use this template to document test results:

```markdown
# VOS Sync E2E Test Report

**Date**: 2026-02-11
**Tester**: [Your Name]
**Device**: [Android version, device type]
**Build**: [App version, Git commit]
**Server**: [Docker/Manual, Version]

## Executive Summary

- **Total Tests**: 12 + 5 edge cases = 17
- **Passed**: __
- **Failed**: __
- **Skipped**: __
- **Status**: PASS / WARN / FAIL

## Test Results

| ID | Test Case | Result | Duration | Notes |
|----|-----------|---------|----|-------|
| TC-01 | Connection Success | PASS | 2.3s | Expected result achieved |
| TC-02 | Wrong Host | PASS | 30.1s | Timeout as expected |
| TC-03 | Wrong Credentials | FAIL | - | Error message was "Auth failed", not "Auth denied" |
| ...

## Detailed Findings

### TC-01: Connection Success ✓
- Dot turned GREEN after 2.3s
- Logcat shows successful SFTP connection
- No errors

### TC-03: Wrong Credentials ✗
- Error message text incorrect
- **Impact**: Minor (UX clarity only)
- **Recommendation**: Update error message string
- **Blocking**: No

## Issues Found

| ID | Severity | Title | Description | Status |
|----|----------|--------|-------------|--------|
| BUG-01 | Medium | Wrong error message text | TC-03: "Auth failed" instead of expected "Authentication failed" | OPEN |
| BUG-02 | Low | Progress bar jittery | TC-11: Animation stutters on large files | OPEN |

## Recommendations

1. Fix error message string (low priority)
2. Optimize progress bar animation (cosmetic)
3. Add retry UI for network interruption (feature request)

## Signature

Tester: _________________   Date: ___________
```

---

## 10. Related Documentation

- **VOS Distribution & Handler Dispatch**: `docs/fixes/VoiceOSCore/VoiceOSCore-Fix-VOSDistributionAndDispatch-260211-V1.md`
- **DB-Driven Voice Commands Phase 2**: `docs/fixes/VoiceOSCore/VoiceOSCore-Fix-Phase2DBDrivenCommands-260211-V1.md`
- **4-Tier Voice Enablement**: `docs/analysis/VoiceOSCore/VoiceOSCore-Analysis-4TierVoiceEnablement-260211-V1.md`
- **Developer Manual Chapter 93 (Voice Pipeline)**: `Docs/MasterDocs/NewAvanues-Developer-Manual/Developer-Manual-Chapter93-VoiceCommandPipeline.md`
- **Developer Manual Chapter 95 (VOS Distribution)**: `Docs/MasterDocs/NewAvanues-Developer-Manual/Developer-Manual-Chapter95-VOSDistributionAndHandlerDispatch.md`

---

## 11. Appendix: Sample Test Files

### A. Sample en-US.app.vos (62 commands)

```json
{
  "version": "2.1",
  "domain": "app",
  "locale": "en-US",
  "generatedAt": "2026-02-11T10:00:00Z",
  "commands": [
    {
      "id": "navigate_home",
      "category": "NAVIGATION",
      "phrases": ["go home", "home screen", "return home"],
      "description": "Navigate to home screen"
    },
    {
      "id": "play",
      "category": "MEDIA",
      "phrases": ["play", "play music", "start playing"],
      "description": "Play current media"
    }
  ],
  "category_map": {
    "NAVIGATION": 0,
    "MEDIA": 1,
    "DEVICE": 2
  },
  "action_map": {
    "navigate_forward": 0,
    "navigate_back": 1,
    "play": 2
  },
  "meta_map": {
    "locales": ["en-US", "es-ES", "fr-FR", "de-DE", "hi-IN"],
    "platform": "android"
  }
}
```

### B. Sample en-US.web.vos (45 commands)

```json
{
  "version": "2.1",
  "domain": "web",
  "locale": "en-US",
  "generatedAt": "2026-02-11T09:30:00Z",
  "commands": [
    {
      "id": "click_search",
      "category": "BROWSER",
      "phrases": ["search", "find", "look for"],
      "description": "Click search box"
    },
    {
      "id": "scroll_down",
      "category": "GESTURE",
      "phrases": ["scroll down", "page down", "next"],
      "description": "Scroll page down"
    }
  ],
  "category_map": {
    "BROWSER": 6,
    "GESTURE": 7
  },
  "action_map": {
    "click_search": 100,
    "scroll_down": 101
  },
  "meta_map": {
    "sourceUrl": "web-scraper-v2.0",
    "platform": "universal"
  }
}
```

---

**Document Version**: V1
**Date**: 2026-02-11
**Author**: Manoj Jhawar, Aman Jhawar (Intelligent Devices LLC)
**Status**: Ready for execution
**Next Update**: After test execution with results
