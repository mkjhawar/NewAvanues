# Questions for Review

## Legal Questions

1. **License Type**: What type of license should be used for VOS3?
   - Commercial proprietary?
   - Dual licensing (open source + commercial)?
   - Custom license terms?

2. **Copyright Notice**: Should it be:
   - Augmentalis?
   - Manoj Jhawar?
   - Both?

3. **Third-Party Licenses**: Need to review licenses for:
   - Vosk (Apache 2.0)
   - Android libraries
   - Kotlin libraries

## Technical Questions

1. **Package Name**: Confirm `com.augmentalis.voiceos` is correct?

2. **Version Number**: Start with 3.0.0 or continue from legacy version?

3. **Target SDK**: Currently targeting SDK 34 (Android 14), is this correct?

4. **Minimum SDK**: Currently set to 26 (Android 8.0), should it be lower?

## Business Questions

1. **App Distribution**: 
   - Google Play Store?
   - Direct APK distribution?
   - Enterprise distribution?

2. **Update Mechanism**:
   - Auto-updates?
   - Manual updates only?
   - Staged rollouts?

3. **Analytics/Telemetry**:
   - Should we include any analytics?
   - Crash reporting service?
   - Usage metrics?

## Files to Delete (Pending Approval)

From VOS2:
- [ ] .warp.md (now in /Coding root)
- [ ] AiInstructions/ folder (now in /Coding root)

## Development Questions

1. **Git Repository**:
   - GitHub, GitLab, or other?
   - Public or private?
   - Branch protection rules?

2. **CI/CD Pipeline**:
   - GitHub Actions?
   - GitLab CI?
   - Other?

3. **Code Signing**:
   - Debug keystore location?
   - Release keystore details?

---

*Please review and provide answers when convenient*