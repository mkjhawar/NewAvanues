# Chapter 7: Troubleshooting & FAQ

**Document:** VoiceOS-AvaLearnLite-UserManual-Ch07
**Version:** 1.0
**Last Updated:** 2025-12-11

---

## 7.1 Common Issues

### 7.1.1 Service Connection Issues

#### Issue: "Service not bound"

**Symptoms:**
- JIT Status shows "Service not bound"
- All statistics show 0
- Buttons don't respond

**Causes:**
- VoiceOS service not running
- Accessibility service disabled
- App needs restart

**Solutions:**

| Step | Action |
|------|--------|
| 1 | Open Settings > Accessibility |
| 2 | Find VoiceOS |
| 3 | Toggle OFF then ON |
| 4 | Return to AvaLearnLite |
| 5 | Press Refresh button |

---

#### Issue: "JIT Status: Paused"

**Symptoms:**
- Status badge shows orange "Paused"
- No new elements discovered
- Statistics not updating

**Causes:**
- JIT manually paused
- Exploration was active
- System resource management

**Solutions:**

| Step | Action |
|------|--------|
| 1 | Press Resume button in JIT card |
| 2 | Verify status changes to "Active" |
| 3 | If still paused, restart AvaLearnLite |

---

#### Issue: Service keeps disconnecting

**Symptoms:**
- Status flickers between Connected/Not bound
- Random disconnections
- Statistics reset to 0

**Causes:**
- Battery optimization killing service
- Low memory conditions
- System power management

**Solutions:**

| Step | Action |
|------|--------|
| 1 | Settings > Apps > AvaLearnLite > Battery |
| 2 | Select "Unrestricted" |
| 3 | Settings > Apps > VoiceOS > Battery |
| 4 | Select "Unrestricted" |
| 5 | Restart both apps |

---

### 7.1.2 Exploration Issues

#### Issue: Exploration doesn't start

**Symptoms:**
- Press Start but nothing happens
- Phase stays IDLE
- No error message

**Causes:**
- Service not connected
- Target app not in foreground
- Previous exploration not stopped

**Solutions:**

| Step | Action |
|------|--------|
| 1 | Verify JIT Status shows "Active" |
| 2 | Open target app first |
| 3 | Switch back to AvaLearnLite |
| 4 | Press Start Exploration |

---

#### Issue: No elements discovered

**Symptoms:**
- Exploration running
- Elements count stays 0
- Coverage stays 0%

**Causes:**
- App uses custom rendering (games)
- WebView-based app
- Accessibility not supported

**Solutions:**

| Step | Action |
|------|--------|
| 1 | Try different screens in the app |
| 2 | Focus on native UI elements |
| 3 | Avoid WebView/game screens |
| 4 | Some apps cannot be learned |

---

#### Issue: Exploration gets stuck

**Symptoms:**
- Phase shows EXPLORING but no progress
- Statistics not updating
- App seems frozen

**Causes:**
- Loop detected
- Heavy screen processing
- Service communication issue

**Solutions:**

| Step | Action |
|------|--------|
| 1 | Press Stop Exploration |
| 2 | Navigate to a different screen |
| 3 | Press Start Exploration again |
| 4 | If persists, restart AvaLearnLite |

---

#### Issue: Login screen keeps appearing

**Symptoms:**
- Warning banner shows repeatedly
- Exploration pauses often
- Same screen detected

**Causes:**
- App has login-like screens
- Security dialogs detected
- False positive detection

**Solutions:**

| Step | Action |
|------|--------|
| 1 | Log into the app manually first |
| 2 | Navigate past login screens |
| 3 | Start exploration after login |

---

### 7.1.3 Export Issues

#### Issue: Export button disabled

**Symptoms:**
- Button is grayed out
- Cannot tap Export
- Hint text shows

**Causes:**
- No elements to export
- Storage permission not granted
- Service not connected

**Solutions:**

| Step | Action |
|------|--------|
| 1 | Complete an exploration first |
| 2 | Check Elements count > 0 |
| 3 | Settings > Apps > AvaLearnLite > Permissions > Storage |
| 4 | Enable storage permission |

---

#### Issue: Export fails

**Symptoms:**
- Error message appears
- No file created
- Export button re-enables

**Causes:**
- Storage full
- Permission error
- Write failure

**Solutions:**

| Step | Action |
|------|--------|
| 1 | Check available storage space |
| 2 | Free up space if needed (> 10MB) |
| 3 | Verify storage permission |
| 4 | Restart app and retry |

---

#### Issue: Commands not working after export

**Symptoms:**
- Export succeeded
- VoiceOS doesn't recognize commands
- App not showing in learned apps

**Causes:**
- VoiceOS not detecting file
- Import failed
- File format issue

**Solutions:**

| Step | Action |
|------|--------|
| 1 | Restart VoiceOS service |
| 2 | Wait 30 seconds |
| 3 | Try voice command again |
| 4 | If still failing, re-export |

---

### 7.1.4 Performance Issues

#### Issue: AvaLearnLite is slow

**Symptoms:**
- UI laggy
- Buttons respond slowly
- Statistics update delays

**Causes:**
- Many elements being processed
- Low device memory
- Background apps

**Solutions:**

| Step | Action |
|------|--------|
| 1 | Close unnecessary background apps |
| 2 | Restart AvaLearnLite |
| 3 | Try exploring simpler screens first |

---

#### Issue: Device gets hot during exploration

**Symptoms:**
- Device temperature rises
- Battery drains faster
- Performance degrades

**Causes:**
- Intensive accessibility processing
- Long exploration sessions
- Many elements on screen

**Solutions:**

| Step | Action |
|------|--------|
| 1 | Take breaks during exploration |
| 2 | Explore in shorter sessions |
| 3 | Let device cool before continuing |

---

#### Issue: Battery drains quickly

**Symptoms:**
- Significant battery drop
- Background drain reported
- VoiceOS listed as high user

**Causes:**
- Continuous accessibility monitoring
- JIT always running
- Normal behavior for learning

**Solutions:**

| Step | Action |
|------|--------|
| 1 | Pause JIT when not exploring |
| 2 | Only enable during learning sessions |
| 3 | This is expected behavior |

---

## 7.2 FAQ

### 7.2.1 General Questions

**Q: What Android version do I need?**

A: Android 14.0 (API 34) or higher is required. Android 15.0 (API 35) is recommended for best performance.

---

**Q: Does AvaLearnLite work offline?**

A: Yes, AvaLearnLite works completely offline. No internet connection is required.

---

**Q: Is my data sent to the cloud?**

A: No. All data stays on your device. Nothing is uploaded to external servers.

---

**Q: Can I learn any app?**

A: Most apps with standard Android UI can be learned. Games, WebView apps, and apps with custom rendering may not work well.

---

**Q: How many apps can I learn?**

A: There is no limit. You can learn as many apps as you have storage space for.

---

### 7.2.2 Exploration Questions

**Q: How long does exploration take?**

A: It depends on the app complexity. Simple apps: 5-10 minutes. Complex apps: 20-30 minutes.

---

**Q: Do I need to tap every button?**

A: No. AvaLearnLite discovers elements automatically. But interacting with elements helps confirm they work.

---

**Q: What if I accidentally tap something dangerous?**

A: The DoNotClick system prevents interaction with dangerous elements like delete or pay buttons.

---

**Q: Can I explore multiple apps at once?**

A: No. Explore one app at a time for best results.

---

**Q: What happens if I get a phone call during exploration?**

A: Exploration pauses. When you return to the app, you can continue or restart.

---

### 7.2.3 Export Questions

**Q: Where are my exported files?**

A: Files are in:
```
/Android/data/com.augmentalis.avalearnlite/files/learned_apps/
```

---

**Q: Can I share exported files with others?**

A: Yes, .vos files can be shared. Recipients need AvaLearnLite or VoiceOS to use them.

---

**Q: Do I need to re-export after app updates?**

A: Often yes. If the app's UI changed, re-exploration may be needed.

---

**Q: Can I edit the exported files?**

A: The user edition exports encrypted files. Use AvaLearnPro (developer edition) for editable exports.

---

### 7.2.4 Safety Questions

**Q: Can AvaLearnLite delete my data?**

A: No. The DoNotClick system prevents interaction with delete buttons and similar dangerous elements.

---

**Q: Will AvaLearnLite log me out of apps?**

A: No. Login and logout buttons are protected by the DoNotClick system.

---

**Q: Is it safe to explore banking apps?**

A: AvaLearnLite protects against dangerous actions, but we recommend caution with sensitive apps. Credentials are never captured.

---

**Q: Can AvaLearnLite make purchases?**

A: No. Purchase buttons are protected by the DoNotClick system.

---

### 7.2.5 VoiceOS Integration Questions

**Q: Why don't my voice commands work?**

A: Common causes:
1. VoiceOS service not running
2. File not imported yet (wait 30 seconds)
3. Command phrasing not recognized (try synonyms)
4. App UI changed since learning

---

**Q: How do I see what commands were learned?**

A: In VoiceOS, say "Show commands for [app name]" or check Settings > Commands.

---

**Q: Can I customize the voice commands?**

A: In the user edition, commands are auto-generated. Use AvaLearnPro for customization.

---

## 7.3 Error Messages

### 7.3.1 Error Reference

| Error | Meaning | Solution |
|-------|---------|----------|
| "Service not available" | VoiceOS not running | Enable VoiceOS accessibility |
| "Cannot bind to service" | Communication failed | Restart both apps |
| "Storage permission required" | Need file access | Grant storage permission |
| "Export failed: IO error" | Write failed | Check storage space |
| "No elements to export" | Nothing learned | Complete exploration first |
| "Screen hash collision" | Internal error | Report to support |

### 7.3.2 Toast Messages

| Toast | Meaning |
|-------|---------|
| "Connected to JIT" | Service bound successfully |
| "Disconnected" | Service connection lost |
| "Exploration started" | Now learning |
| "Exploration stopped" | Learning paused |
| "Export complete" | File saved successfully |
| "Login detected" | Auth screen found |

---

## 7.4 Diagnostic Steps

### 7.4.1 Basic Diagnostics

```
1. Check JIT Status card
   - Should show "Active" (green)
   - If not, restart VoiceOS

2. Check element count
   - Should be > 0 after exploring
   - If 0, app may not support accessibility

3. Check export button
   - Should be enabled after exploration
   - If disabled, check permissions
```

### 7.4.2 Advanced Diagnostics

```
1. Check accessibility services
   Settings > Accessibility > VoiceOS
   - Should be enabled
   - Toggle off/on if issues

2. Check app permissions
   Settings > Apps > AvaLearnLite > Permissions
   - Storage: Allowed
   - Other: As needed

3. Check battery settings
   Settings > Apps > AvaLearnLite > Battery
   - Should be "Unrestricted"
```

---

## 7.5 Getting Help

### 7.5.1 Support Resources

| Resource | Access |
|----------|--------|
| This manual | In-app or online |
| FAQ | Chapter 7.2 |
| Community forum | forums.augmentalis.com |
| Email support | support@augmentalis.com |

### 7.5.2 Reporting Issues

When reporting issues, include:

| Information | Where to Find |
|-------------|---------------|
| Android version | Settings > About phone |
| AvaLearnLite version | Settings > Apps > AvaLearnLite |
| VoiceOS version | Settings > Apps > VoiceOS |
| Steps to reproduce | Your actions before the issue |
| Error messages | Screenshots if possible |

### 7.5.3 Feature Requests

Submit feature requests at:
- GitHub: github.com/augmentalis/avalearnlite
- Email: features@augmentalis.com

---

## 7.6 Next Steps

For terminology and quick reference, see [Chapter 8: Glossary & Reference](./08-Glossary-Reference.md).

---

**End of Chapter 7**
