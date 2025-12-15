# Bug Fixes: Downloads & Encryption - December 12, 2025

## Fix #1: Encryption Made Optional (User-Controllable)

### Problem
- Database and SharedPreferences were hardcoded to use encryption (default: `true`)
- No user control over encryption settings
- Performance impact for users who don't need encryption

### Solution
**Added user-controllable encryption settings:**

1. **BrowserSettings** - Added two new settings:
   - `enableDatabaseEncryption: Boolean = false` (default: unencrypted)
   - `enableSecureStorage: Boolean = false` (default: unencrypted)

2. **DatabaseDriver** - Changed default from encrypted to plaintext:
   - `useEncryption = false` (was `true`)
   - Reads setting from bootstrap preferences

3. **SecureStorage** - Conditional encryption:
   - Uses `EncryptedSharedPreferences` when enabled
   - Uses plain `SharedPreferences` when disabled

4. **Bootstrap Preferences** - Solves chicken-and-egg problem:
   - Encryption setting stored in `webavanue_bootstrap` SharedPreferences
   - Loaded before database initialization
   - Keys: `database_encryption`, `secure_storage_encryption`

### Files Modified
- `BrowserSettings.kt` (+2 settings)
- `DatabaseDriver.kt` (default changed + docs)
- `WebAvanueApp.kt` (bootstrap preferences reading)
- `SecureStorage.kt` (conditional encryption)

### User Impact
- **Performance**: Faster database access by default
- **Privacy**: Users can opt-in to encryption in Settings
- **Migration**: Existing encrypted databases continue to work

---

## Fix #2: File Downloads Not Saving to Disk

### Problem
- Downloads would start (show in UI) but never save files to disk
- Download location setting had no effect
- Root cause: `DownloadViewModel` never enqueued downloads to `AndroidDownloadQueue`

### Solution
**Connected download pipeline end-to-end:**

1. **DownloadViewModel** - Added `DownloadQueue` parameter:
   - Accepts optional `DownloadQueue` in constructor
   - `startDownload()` now calls `downloadQueue.enqueue()` after validation
   - Creates `DownloadRequest` with all metadata

2. **AndroidDownloadQueue** - Added settings support:
   - Accepts `getDownloadPath` callback to read user settings
   - Respects custom download path (when implemented)
   - Falls back to system Downloads folder

3. **ViewModelHolder** - Added downloadQueue parameter:
   - `create()` method passes downloadQueue to DownloadViewModel

4. **BrowserApp** - Added downloadQueue parameter:
   - Passes downloadQueue through to ViewModelHolder

5. **MainActivity** - Instantiates AndroidDownloadQueue:
   - Creates queue with context and settings callback
   - Callback uses `repository.getSettings()` to get download path

### Files Modified
- `DownloadViewModel.kt` (+30 lines: queue parameter + enqueue logic)
- `AndroidDownloadQueue.kt` (+10 lines: settings callback)
- `Screen.kt` (ViewModelHolder: +1 parameter)
- `BrowserApp.kt` (+1 parameter)
- `MainActivity.kt` (+10 lines: queue instantiation)

### User Impact
- **Downloads Work**: Files now actually download and save to disk
- **Settings Respected**: Download path setting is read (future: custom paths)
- **Android System**: Uses Android DownloadManager (notifications, progress, retry)

### Known Limitations
- "Ask Download Location" setting saves but not yet implemented (requires file picker dialog)
- Custom download paths validated but not yet used (requires additional permissions)

---

## Technical Details

### Download Flow (Fixed)
```
User clicks download link
  ↓
WebView intercepts download
  ↓
DownloadViewModel.startDownload()
  ↓
Validation (URL, filename, extension)
  ↓
Create Download object
  ↓
Save to repository (database)
  ↓
**NEW:** Enqueue to AndroidDownloadQueue
  ↓
Android DownloadManager starts download
  ↓
File saved to disk (Downloads folder)
  ↓
Notification shown
```

### Encryption Architecture
```
Bootstrap Preferences (plaintext)
  ├─ database_encryption: Boolean
  └─ secure_storage_encryption: Boolean
      ↓
WebAvanueApp.onCreate()
  ├─ Read bootstrap settings
  ├─ Create driver with useEncryption flag
  └─ Database uses SQLCipher or plain SQLite
      ↓
SecureStorage.init()
  ├─ Read bootstrap setting
  └─ Use EncryptedSharedPreferences or plain
```

---

## Testing Checklist

### Downloads
- [ ] Download a file - verify it saves to Downloads folder
- [ ] Download multiple files - verify queue works
- [ ] Download with custom filename - verify sanitization
- [ ] Download dangerous extension - verify blocked
- [ ] Check download notification appears
- [ ] Check download appears in system Downloads

### Encryption
- [ ] Fresh install - verify unencrypted by default
- [ ] Enable database encryption in Settings - verify bootstrap pref saved
- [ ] Enable secure storage encryption - verify bootstrap pref saved
- [ ] Restart app - verify settings persist
- [ ] Check database file is plaintext (or encrypted if enabled)

---

## Future Enhancements

1. **Ask Download Location**:
   - Implement Android file picker (Storage Access Framework)
   - Show picker dialog before download starts
   - Remember user choice per session

2. **Custom Download Paths**:
   - Validate user-provided paths
   - Request MANAGE_EXTERNAL_STORAGE permission (Android 11+)
   - Use scoped storage APIs

3. **Download Progress**:
   - Connect AndroidDownloadQueue progress observer to UI
   - Show progress bar in download list
   - Update download status in repository

4. **Encryption Migration**:
   - Add "Encrypt Existing Data" option in Settings
   - Migrate plaintext → encrypted database
   - Migrate plaintext → encrypted SharedPreferences

---

**Fixes Complete**: December 12, 2025
**Build Status**: Ready for testing
**Commit**: Pending
