# Security Policy

## Overview

WebAvanue takes security seriously. This document outlines our security features, supported versions, and vulnerability reporting process.

## Security Features

### 1. Database Encryption (AES-256)

WebAvanue uses **SQLCipher** for full database encryption with AES-256-CBC.

**What's Protected:**
- Browsing history
- Saved favorites/bookmarks
- Downloaded file metadata
- Browser settings
- Site permissions

**Implementation:**
```kotlin
// Database initialized with encryption key
val driver = AndroidSqliteDriver(
    BrowserDatabase.Schema,
    context,
    "webavanue.db",
    callback = SQLCipherCallback("your-secure-key")
)
```

**Key Management:**
- Encryption key derived from device-specific identifiers
- Key stored in Android KeyStore (hardware-backed when available)
- Key never stored in plaintext

**Security Level:** AES-256-CBC (256-bit encryption)

### 2. Secure Credential Storage

Sensitive data (passwords, tokens) stored in **EncryptedSharedPreferences**.

**What's Protected:**
- Sync credentials (future)
- OAuth tokens (future)
- API keys

**Implementation:**
```kotlin
val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "webavanue_secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

**Security Level:** AES-256-GCM (Galois/Counter Mode)

### 3. Network Security Configuration

Certificate pinning and TLS enforcement via Android Network Security Config.

**Configuration** (`res/xml/network_security_config.xml`):
```xml
<network-security-config>
    <!-- Enforce HTTPS for all connections -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>

    <!-- Certificate pinning for API endpoints (future) -->
    <domain-config>
        <domain includeSubdomains="true">api.webavanue.com</domain>
        <pin-set>
            <pin digest="SHA-256">base64-encoded-cert-hash</pin>
            <pin digest="SHA-256">backup-cert-hash</pin>
        </pin-set>
    </domain-config>
</network-security-config>
```

**Security Features:**
- HTTPS enforcement (no cleartext traffic)
- Certificate pinning for critical domains
- TLS 1.2+ required
- Strong cipher suites only

### 4. Code Obfuscation (ProGuard/R8)

Release builds use ProGuard for code shrinking and obfuscation.

**Configuration** (`proguard-rules.pro`):
```proguard
# Obfuscate class and method names
-repackageclasses 'o'
-allowaccessmodification

# Keep data models (for serialization)
-keepclassmembers class com.augmentalis.webavanue.domain.model.** {
    <fields>;
}

# Remove logging in release
-assumenosideeffects class io.github.aakira.napier.Napier {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
```

**Security Benefits:**
- Reverse engineering difficulty increased
- Binary size reduced (side benefit)
- Debug logs removed from release builds

### 5. Permission Management

WebAvanue follows principle of least privilege.

**Required Permissions:**
```xml
<!-- Essential -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- File downloads -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />  <!-- Scoped storage on API 29+ -->

<!-- Optional (runtime request only when needed) -->
<uses-permission android:name="android.permission.CAMERA" />  <!-- For QR code, AR -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />  <!-- For voice commands -->
```

**Runtime Permission Handling:**
- Camera: Only requested when user scans QR code or uses AR
- Microphone: Only requested when user enables voice commands
- Storage: Scoped storage on Android 10+

### 6. WebView Security Hardening

WebView configured with security-first defaults.

**Security Settings:**
```kotlin
webView.settings.apply {
    // Disable unsafe features
    allowFileAccessFromFileURLs = false
    allowUniversalAccessFromFileURLs = false
    allowContentAccess = false  // Prevent content:// access

    // Enable safe browsing
    safeBrowsingEnabled = true

    // Mixed content blocking
    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

    // JavaScript enabled but sandboxed
    javaScriptEnabled = true
    javaScriptCanOpenWindowsAutomatically = false

    // Secure defaults
    savePassword = false  // Don't save passwords in WebView
    saveFormData = false  // Don't save form data
}
```

**Security Features:**
- Safe Browsing API integration (phishing/malware detection)
- JavaScript sandboxing
- Same-origin policy enforcement
- Mixed content blocking (no HTTP on HTTPS pages)

### 7. Input Validation

All user input validated before processing.

**URL Validation:**
```kotlin
object UrlValidation {
    fun validate(url: String): ValidationResult {
        // Sanitize input
        val trimmed = url.trim()

        // Reject dangerous protocols
        if (trimmed.startsWith("file://") ||
            trimmed.startsWith("javascript:") ||
            trimmed.startsWith("data:")
        ) {
            return Invalid(SecurityError("Unsafe URL scheme"))
        }

        // Ensure HTTPS (upgrade HTTP automatically)
        val normalizedUrl = if (trimmed.startsWith("http://")) {
            "https://" + trimmed.removePrefix("http://")
        } else {
            trimmed
        }

        return Valid(normalizedUrl)
    }
}
```

**Download Validation:**
```kotlin
// Validate filename to prevent path traversal
fun sanitizeFilename(filename: String): String {
    return filename
        .replace(Regex("[^a-zA-Z0-9._-]"), "_")  // Remove unsafe chars
        .take(255)  // Limit length
}

// Validate URL scheme (only HTTP(S))
if (!url.startsWith("http://") && !url.startsWith("https://")) {
    throw SecurityException("Only HTTP(S) downloads allowed")
}
```

### 8. Privacy Controls

User has full control over privacy settings.

**Privacy Settings:**
- **Do Not Track**: Sends DNT header with requests
- **Block Trackers**: Blocks known tracking domains
- **Block Ads**: Blocks known ad networks
- **Clear on Exit**: Optionally clear history/cookies on exit
- **Incognito Mode**: Private browsing (no history saved)

**Data Collection:**
- WebAvanue does **NOT** collect or transmit user data
- No analytics, no telemetry, no tracking
- All data stays on device

### 9. Crash Reporting (Sentry)

Crash reports sent to Sentry for debugging (opt-in only).

**Privacy Measures:**
- User consent required before enabling
- URLs sanitized (only domain kept, no paths/queries)
- No personally identifiable information (PII)
- Can be disabled in settings

**Sentry Configuration:**
```kotlin
SentryAndroid.init(context) { options ->
    options.dsn = "your-sentry-dsn"
    options.environment = if (BuildConfig.DEBUG) "debug" else "production"

    // Privacy: Scrub PII
    options.beforeSend = SentryOptions.BeforeSendCallback { event, hint ->
        // Remove sensitive data
        event.request?.url = event.request?.url?.let { sanitizeUrl(it) }
        event.user = null  // No user identification
        event
    }
}
```

## Supported Versions

We provide security updates for the following versions:

| Version | Supported          | End of Support |
|---------|--------------------|----------------|
| 4.0.x   | ✅ Yes (Current)   | TBD            |
| 3.x     | ⚠️ Critical only   | 2025-06-30     |
| < 3.0   | ❌ No              | Ended          |

**Note**: Only the latest major version receives full security updates. Previous major version receives critical security fixes for 6 months after new major release.

## Reporting a Vulnerability

If you discover a security vulnerability, please report it responsibly.

### ⚠️ DO NOT

- ❌ Open a public GitHub issue
- ❌ Post details on social media
- ❌ Exploit the vulnerability

### ✅ DO

1. **Email us privately**: security@webavanue.com (GPG key below)
2. **Include details**:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)
3. **Allow time for response**: We aim to respond within 48 hours

### GPG Public Key

```
-----BEGIN PGP PUBLIC KEY BLOCK-----

[Your GPG public key here]

-----END PGP PUBLIC KEY BLOCK-----
```

### Disclosure Timeline

1. **Day 0**: You report the vulnerability
2. **Day 1-2**: We acknowledge receipt and begin investigation
3. **Day 3-14**: We develop and test a fix
4. **Day 15**: We release a security update
5. **Day 30**: Public disclosure (coordinated with reporter)

### Bug Bounty

We currently do not offer a bug bounty program. However, we deeply appreciate security researchers and will:

- Credit you in our security advisories (with your permission)
- Provide a special "Security Contributor" badge
- Send swag (t-shirt, stickers) as a thank you

## Security Best Practices for Users

### 1. Keep WebAvanue Updated

Always use the latest version for security patches.

**Update Channels:**
- Google Play Store (automatic updates)
- GitHub Releases (manual download)
- F-Droid (FOSS builds)

### 2. Enable Security Features

**Recommended Settings:**
- ✅ Enable "Block Ads"
- ✅ Enable "Block Trackers"
- ✅ Enable "Do Not Track"
- ✅ Enable "Safe Browsing"
- ✅ Disable "JavaScript" for untrusted sites (if needed)
- ✅ Enable "Clear History on Exit" (for extra privacy)

**Access**: Settings → Privacy & Security

### 3. Use Strong Passwords

If using sync features (future):
- Use unique, strong passwords (12+ characters)
- Enable two-factor authentication (2FA)
- Use a password manager

### 4. Be Cautious with Permissions

**Before granting permissions:**
- Camera: Only grant when scanning QR codes or using AR
- Microphone: Only grant when using voice commands
- Storage: Grant only when downloading files

**Review permissions**: Settings → Apps → WebAvanue → Permissions

### 5. Verify HTTPS

Always check for HTTPS lock icon before entering sensitive data.

**Warning signs:**
- ⚠️ "Not Secure" warning
- ⚠️ Invalid certificate
- ⚠️ Mixed content warning

### 6. Avoid Phishing

WebAvanue uses Google Safe Browsing to warn about phishing sites.

**Phishing indicators:**
- Misspelled URLs (goog1e.com instead of google.com)
- Suspicious emails asking for credentials
- Urgent requests for personal information

### 7. Use Incognito Mode

For sensitive browsing (banking, health, etc.):
- Tap **New Incognito Tab** in menu
- History and cookies not saved
- Close all incognito tabs when done

### 8. Regular Security Audits

Periodically review:
- **Site Permissions**: Settings → Site Permissions
- **Saved Passwords**: Settings → Passwords (future)
- **Download History**: Settings → Downloads

## Security Audits & Compliance

### Third-Party Audits

WebAvanue has not yet undergone a formal third-party security audit.

**Planned:** Q2 2026 - External security audit by reputable firm

### Compliance

- **GDPR**: Compliant (no data collection)
- **CCPA**: Compliant (no data selling)
- **COPPA**: Not applicable (app not targeted at children <13)

### Open Source

WebAvanue is open source, allowing community security review.

**Source Code**: https://github.com/yourusername/NewAvanues-WebAvanue

**License**: [Your License]

## Known Limitations

### 1. WebView Vulnerabilities

WebAvanue relies on Android's WebView component for rendering web content. WebView vulnerabilities are patched by Google via Play System Updates.

**Mitigation:**
- Keep Android OS updated
- Enable automatic Play System Updates
- WebView updated independently of OS on Android 5+

### 2. Side-Channel Attacks

Like all browsers, WebAvanue may be vulnerable to timing attacks, speculative execution attacks (Spectre/Meltdown), and other side-channel attacks.

**Mitigation:**
- Site Isolation (enabled where supported)
- JavaScript sandboxing
- Same-origin policy enforcement

### 3. Zero-Day Vulnerabilities

Unknown vulnerabilities may exist in dependencies (SQLDelight, Kotlin, AndroidX, etc.).

**Mitigation:**
- Monitor security advisories
- Update dependencies regularly
- Automated dependency scanning (Dependabot)

## Security Resources

### For Developers

- [OWASP Mobile Security Project](https://owasp.org/www-project-mobile-security/)
- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices)
- [SQLCipher Documentation](https://www.zetetic.net/sqlcipher/documentation/)

### For Users

- [Google Safe Browsing](https://safebrowsing.google.com/)
- [EFF Privacy Tools](https://www.eff.org/pages/tools)
- [Have I Been Pwned](https://haveibeenpwned.com/)

## Contact

**Security Issues**: security@webavanue.com

**General Support**: support@webavanue.com

**Twitter**: @WebAvanue

---

**Last Updated**: 2025-12-12
**Version**: 1.0
