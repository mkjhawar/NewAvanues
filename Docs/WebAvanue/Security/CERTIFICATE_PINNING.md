# Certificate Pinning - WebAvanue

**Status:** Active
**Last Updated:** 2025-12-11
**Security Level:** High

---

## Overview

Certificate pinning enhances WebAvanue's security by preventing MITM (Man-in-the-Middle) attacks through public key pinning. This ensures that connections are only established with servers presenting expected certificates.

### Security Benefits

| Threat | Mitigation |
|--------|------------|
| MITM Attacks | Pinned certificates reject proxy/interception attempts |
| Rogue CAs | Prevents unauthorized certificate authorities |
| Certificate Substitution | Detects certificate tampering |
| Network Interception | Blocks corporate/ISP SSL interception |

---

## Implementation

### Configuration File

**Location:** `/android/apps/webavanue/app/src/main/res/xml/network_security_config.xml`

### Pin Structure

```xml
<domain-config>
    <domain includeSubdomains="true">api.webavanue.com</domain>
    <pin-set expiration="2026-12-31">
        <!-- Primary certificate pin -->
        <pin digest="SHA-256">PRIMARY_PIN_BASE64</pin>
        <!-- Backup certificate pin -->
        <pin digest="SHA-256">BACKUP_PIN_BASE64</pin>
    </pin-set>
</domain-config>
```

### Handler Implementation

**Location:** `/Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/network/CertificatePinningHandler.kt`

**Usage:**
```kotlin
try {
    // Network operation
} catch (e: Exception) {
    if (CertificatePinningHandler.isPinningFailure(e)) {
        CertificatePinningHandler.handlePinningFailure(url, e)
        // Show user error message
        showError(CertificatePinningHandler.getUserMessage(url, e))
    }
}
```

---

## Extracting Certificate Pins

### Method 1: OpenSSL (Recommended)

```bash
# Extract SHA-256 fingerprint from domain
openssl s_client -connect domain.com:443 -servername domain.com < /dev/null 2>/dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | base64

# Example output:
# 47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=
```

### Method 2: Chrome DevTools

1. Open Chrome DevTools (F12)
2. Navigate to **Security** tab
3. Click **View certificate**
4. Navigate to **Details** tab
5. Export certificate as DER/PEM
6. Use OpenSSL to extract public key hash:
   ```bash
   openssl x509 -in certificate.pem -pubkey -noout \
     | openssl pkey -pubin -outform der \
     | openssl dgst -sha256 -binary \
     | base64
   ```

### Method 3: Android Studio Network Inspector

1. Run app in debug mode
2. Open **View > Tool Windows > App Inspection**
3. Navigate to **Network Inspector**
4. Capture HTTPS request
5. View certificate chain and extract public key hash

---

## Pin Rotation Process

### Pre-Rotation (90 days before expiration)

1. **Obtain New Certificate**
   ```bash
   # Generate new certificate or obtain from CA
   # Extract new pin
   NEW_PIN=$(openssl s_client -connect domain.com:443 ...)
   ```

2. **Add Backup Pin**
   ```xml
   <pin-set expiration="2027-12-31">
       <pin digest="SHA-256">OLD_PIN</pin>
       <pin digest="SHA-256">NEW_PIN</pin>  <!-- Add backup -->
   </pin-set>
   ```

3. **Release App Update**
   - Deploy app with both old + new pins
   - Ensure 90%+ user adoption before rotation

### Rotation (After 90% adoption)

4. **Deploy New Certificate**
   - Update server with new certificate
   - Both old and new certificates work (dual-pin)

5. **Monitor for Failures**
   ```bash
   # Check logs for pinning failures
   adb logcat | grep "CertPinning"
   ```

### Post-Rotation (30 days after)

6. **Remove Old Pin**
   ```xml
   <pin-set expiration="2027-12-31">
       <pin digest="SHA-256">NEW_PIN</pin>  <!-- Only new pin -->
   </pin-set>
   ```

7. **Update Documentation**
   - Document rotation date
   - Update expiration tracking

---

## Pin Expiration Tracking

### Expiration Calendar

| Domain | Current Pin | Expiration | Backup Pin | Next Rotation |
|--------|-------------|------------|------------|---------------|
| api.webavanue.com | PRIMARY | 2026-12-31 | BACKUP | 2026-09-30 |
| telemetry.webavanue.com | PRIMARY | 2026-12-31 | BACKUP | 2026-09-30 |

### Automated Reminders

Create calendar events 90 days before expiration:
```bash
# Add to cron or CI/CD
# Send notification 90 days before expiration
0 0 * * * /scripts/check-pin-expiration.sh
```

---

## Testing Certificate Pinning

### Valid Certificate Test

```bash
# Should succeed
curl -v https://api.webavanue.com
```

**Expected:** Connection succeeds, app functions normally

### Invalid Certificate Test (Manual)

1. **Modify network_security_config.xml:**
   ```xml
   <!-- Change pin to invalid value -->
   <pin digest="SHA-256">INVALID_PIN_FOR_TESTING</pin>
   ```

2. **Run app and attempt connection**
3. **Expected:**
   - Connection fails with `SSLHandshakeException`
   - `CertificatePinningHandler` logs error
   - User sees security error message

4. **Verify logs:**
   ```bash
   adb logcat | grep "CertPinning"
   # Expected: SSL handshake failed - Potential MITM attack
   ```

5. **Restore valid pin after testing**

### Automated Test

```kotlin
@Test
fun testCertificatePinningFailure() {
    // Mock SSLHandshakeException
    val error = SSLHandshakeException("Pin verification failed")

    // Verify handler detects pinning failure
    assertTrue(CertificatePinningHandler.isPinningFailure(error))

    // Verify error message
    val message = CertificatePinningHandler.getUserMessage("https://api.test.com", error)
    assertTrue(message.contains("Security Error"))
}
```

---

## Production Deployment Checklist

### Pre-Deployment

- [ ] Extract actual certificate pins for all production domains
- [ ] Replace placeholder pins in `network_security_config.xml`
- [ ] Add backup pins for certificate rotation
- [ ] Set expiration date (12 months from deployment)
- [ ] Test pinning with valid certificates
- [ ] Test pinning failure handling
- [ ] Document pin values in secure location (password manager)

### Deployment

- [ ] Deploy app with certificate pinning enabled
- [ ] Monitor logs for pinning failures (first 48 hours)
- [ ] Verify no false positives
- [ ] Set calendar reminders for pin rotation (90 days before expiration)

### Post-Deployment

- [ ] Document deployment date
- [ ] Update pin expiration tracking
- [ ] Create rotation plan
- [ ] Schedule quarterly pin validation

---

## Troubleshooting

### Issue: Pinning Failures on Valid Connections

**Symptoms:**
- App fails to connect despite valid certificate
- Logs show `SSLHandshakeException`

**Solutions:**

1. **Verify Pin Accuracy**
   ```bash
   # Re-extract pin and compare
   openssl s_client -connect domain.com:443 ...
   ```

2. **Check Certificate Chain**
   - Pin might be for wrong certificate in chain
   - Pin leaf certificate (server cert), not intermediate

3. **Verify Domain Match**
   - Ensure domain in config matches actual domain
   - Check subdomain configuration (`includeSubdomains`)

4. **Check Expiration**
   - Verify `pin-set expiration` hasn't passed
   - Extend expiration if needed

### Issue: Certificate Rotation Breaking App

**Symptoms:**
- App stops working after server certificate renewal
- Widespread pinning failures

**Solutions:**

1. **Emergency Release (Option 1: Remove Pinning)**
   ```xml
   <!-- Temporarily disable pinning for affected domain -->
   <domain-config>
       <domain includeSubdomains="true">api.webavanue.com</domain>
       <!-- Remove pin-set -->
   </domain-config>
   ```

2. **Emergency Release (Option 2: Add New Pin)**
   ```xml
   <pin-set expiration="2027-12-31">
       <pin digest="SHA-256">OLD_PIN</pin>
       <pin digest="SHA-256">NEW_PIN</pin>  <!-- Add new cert pin -->
   </pin-set>
   ```

3. **Prevention:**
   - Always maintain backup pin
   - Start rotation 90 days before expiration
   - Test rotation in staging environment

---

## Security Best Practices

### Pin Selection

1. **Pin Leaf Certificate** (Recommended)
   - Most specific, highest security
   - Requires app update on certificate renewal
   - Best for apps with regular updates

2. **Pin Intermediate Certificate** (Alternative)
   - Less frequent rotation (2-5 years)
   - Allows server certificate rotation without app update
   - Slightly lower security

3. **Always Include Backup Pin**
   - Enables certificate rotation without breaking app
   - Must be from different CA or different key pair

### Security vs. Flexibility

| Approach | Security | Flexibility | Update Frequency |
|----------|----------|-------------|------------------|
| Leaf Cert Pin | High | Low | Every renewal (1-2 years) |
| Intermediate Pin | Medium | Medium | CA rotation (2-5 years) |
| Root Pin | Low | High | Rarely (5-10 years) |

**Recommendation for WebAvanue:** Pin leaf certificate with backup pin

---

## WebAvanue-Specific Considerations

### Browser App Architecture

WebAvanue is a **browser application** - it doesn't have a traditional backend API. Certificate pinning applies to:

1. **Future Infrastructure:**
   - CDN for browser resources
   - Analytics/telemetry services
   - Update servers
   - Authentication providers

2. **Current State:**
   - Placeholder pins in configuration
   - Real pins added when services deployed

3. **User-Browsed Content:**
   - **NOT pinned** - users browse arbitrary domains
   - Pinning only applies to WebAvanue's own infrastructure
   - Android WebView handles standard certificate validation for user content

### When to Implement Real Pins

Implement certificate pinning when adding:
- Remote configuration service
- Analytics/crash reporting
- Content delivery network
- Authentication service
- Update/version check service

---

## References

- **OWASP Certificate Pinning:** https://owasp.org/www-community/controls/Certificate_and_Public_Key_Pinning
- **Android Network Security Config:** https://developer.android.com/privacy-and-security/security-config
- **CWE-295:** Improper Certificate Validation
- **CWE-319:** Cleartext Transmission of Sensitive Information

---

## Change Log

| Date | Change | Author |
|------|--------|--------|
| 2025-12-11 | Initial certificate pinning implementation | Infrastructure Agent |

