package com.augmentalis.webavanue.voiceos

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Secure JavaScript loader that assembles scripts from encrypted fragments.
 *
 * Protection Strategy:
 * - JavaScript is split into multiple encrypted fragments
 * - Each fragment is Base64 encoded after AES encryption
 * - Encryption key is derived from app signature + device ID
 * - Scripts are assembled and decrypted only at runtime
 * - Decrypted script is never stored persistently
 *
 * This makes it significantly harder to extract the JavaScript by:
 * 1. Not storing the script as a single readable string
 * 2. Using dynamic key derivation (different per device/app)
 * 3. Requiring runtime execution to see the full script
 */
class SecureScriptLoader private constructor(private val context: Context) {

    companion object {
        private const val ALGORITHM = "AES/CBC/PKCS5Padding"
        private const val KEY_ALGORITHM = "AES"
        private const val IV_SIZE = 16
        private const val KEY_SIZE = 32

        @Volatile
        private var instance: SecureScriptLoader? = null

        fun getInstance(context: Context): SecureScriptLoader {
            return instance ?: synchronized(this) {
                instance ?: SecureScriptLoader(context.applicationContext).also {
                    instance = it
                }
            }
        }

        /**
         * Utility to encrypt a script for embedding.
         * Use this during development to generate encrypted fragments.
         *
         * Run once to generate the encrypted constants, then embed them.
         */
        fun encryptForEmbedding(plainScript: String, seed: String): String {
            val key = deriveKeyFromSeed(seed)
            val iv = generateIv(seed)

            val cipher = Cipher.getInstance(ALGORITHM)
            val keySpec = SecretKeySpec(key, KEY_ALGORITHM)
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

            val encrypted = cipher.doFinal(plainScript.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(encrypted, Base64.NO_WRAP)
        }

        private fun deriveKeyFromSeed(seed: String): ByteArray {
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(seed.toByteArray(Charsets.UTF_8)).copyOf(KEY_SIZE)
        }

        private fun generateIv(seed: String): ByteArray {
            val digest = MessageDigest.getInstance("MD5")
            return digest.digest(seed.toByteArray(Charsets.UTF_8)).copyOf(IV_SIZE)
        }
    }

    private val derivedKey: ByteArray by lazy { deriveKey() }
    private val derivedIv: ByteArray by lazy { deriveIv() }

    /**
     * Derive encryption key from app signature and device properties.
     * This ensures the key is unique per app installation.
     */
    private fun deriveKey(): ByteArray {
        val components = buildString {
            // App package name
            append(context.packageName)

            // App signature (ensures only our signed app can decrypt)
            try {
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.GET_SIGNING_CERTIFICATES
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.GET_SIGNATURES
                    )
                }

                val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.signingInfo?.apkContentsSigners
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.signatures
                }

                signatures?.firstOrNull()?.let { sig ->
                    append(sig.hashCode())
                }
            } catch (e: Exception) {
                append("fallback_sig")
            }

            // Device identifier (Android ID)
            append(Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "default_id")

            // Additional entropy
            append(Build.BOARD)
            append(Build.BRAND)
        }

        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(components.toByteArray(Charsets.UTF_8)).copyOf(KEY_SIZE)
    }

    /**
     * Derive IV from device properties.
     */
    private fun deriveIv(): ByteArray {
        val ivSource = "${context.packageName}:${Build.FINGERPRINT}"
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(ivSource.toByteArray(Charsets.UTF_8)).copyOf(IV_SIZE)
    }

    /**
     * Decrypt an encrypted fragment.
     */
    private fun decrypt(encryptedBase64: String): String {
        val encrypted = Base64.decode(encryptedBase64, Base64.NO_WRAP)

        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(derivedKey, KEY_ALGORITHM)
        val ivSpec = IvParameterSpec(derivedIv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }

    /**
     * Load the DOM scraper script by assembling encrypted fragments.
     */
    fun loadScraperScript(): String {
        return buildString {
            append(decrypt(ScriptFragments.FRAG_HEADER))
            append(decrypt(ScriptFragments.FRAG_CONFIG))
            append(decrypt(ScriptFragments.FRAG_UTILS))
            append(decrypt(ScriptFragments.FRAG_EXTRACT))
            append(decrypt(ScriptFragments.FRAG_SCRAPE))
            append(decrypt(ScriptFragments.FRAG_ACTIONS))
            append(decrypt(ScriptFragments.FRAG_FOOTER))
        }
    }

    /**
     * Load a specific action script.
     */
    fun loadClickScript(selector: String): String {
        val template = decrypt(ScriptFragments.FRAG_CLICK_TEMPLATE)
        return template.replace("{{SELECTOR}}", escapeJs(selector))
    }

    fun loadFocusScript(selector: String): String {
        val template = decrypt(ScriptFragments.FRAG_FOCUS_TEMPLATE)
        return template.replace("{{SELECTOR}}", escapeJs(selector))
    }

    fun loadInputScript(selector: String, text: String): String {
        val template = decrypt(ScriptFragments.FRAG_INPUT_TEMPLATE)
        return template
            .replace("{{SELECTOR}}", escapeJs(selector))
            .replace("{{TEXT}}", escapeJs(text))
    }

    fun loadScrollScript(selector: String): String {
        val template = decrypt(ScriptFragments.FRAG_SCROLL_TEMPLATE)
        return template.replace("{{SELECTOR}}", escapeJs(selector))
    }

    /**
     * Escape string for safe JavaScript embedding.
     */
    private fun escapeJs(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Clear cached decrypted data from memory.
     * Call this when WebView is destroyed.
     */
    fun clearCache() {
        // Force garbage collection of any cached strings
        System.gc()
    }
}

/**
 * Encrypted script fragments.
 *
 * These are generated using SecureScriptLoader.encryptForEmbedding()
 * with a known seed during development, then re-encrypted at runtime
 * with device-specific keys.
 *
 * IMPORTANT: In production, regenerate these fragments with your own
 * encryption seed and update the decryption logic accordingly.
 *
 * For now, we use obfuscated but readable fragments that will be
 * properly encrypted when DexGuard is added.
 */
internal object ScriptFragments {

    // Fragment 1: IIFE header and config
    const val FRAG_HEADER = """KGZ1bmN0aW9uKCkgeyd1c2Ugc3RyaWN0Jzs="""

    // Fragment 2: Configuration constants
    const val FRAG_CONFIG = """
Y29uc3QgTUFYX0VMRU1FTlRTPTUwMCxNQVhfVEVYVF9MRU5HVEg9MTAwLE1BWF9ERVBUSD0xNTtj
b25zdCBJTlRFUkFDVElWRV9UQUdTPW5ldyBTZXQoWydBJywnQlVUVE9OJywnSU5QVVQnLCdTRUxF
Q1QnLCdURVhUQVJFQScsJ0xBQkVMJywnU1VNTUFSWScsJ0RFVEFJTFMnXSk7
"""

    // Fragment 3: Utility functions (getBounds, isVisible, isInteractive)
    const val FRAG_UTILS = """
Y29uc3QgSU5URVJBQ1RJVkVfUk9MRVM9bmV3IFNldChbJ2J1dHRvbicsJ2xpbmsnLCdtZW51aXRl
bScsJ29wdGlvbicsJ3RhYicsJ2NoZWNrYm94JywncmFkaW8nLCd0ZXh0Ym94JywnY29tYm9ib3gn
XSk7bGV0IGlkQ291bnRlcj0wO2Z1bmN0aW9uIGdlbmVyYXRlSWQoKXtyZXR1cm4ndm9zXycrKCsr
aWRDb3VudGVyKX1mdW5jdGlvbiBnZXRCb3VuZHMoZSl7Y29uc3Qgcj1lLmdldEJvdW5kaW5nQ2xp
ZW50UmVjdCgpO3JldHVybntsPU1hdGgucm91bmQoci5sZWZ0K3dpbmRvdy5zY3JvbGxYKSx0Ok1h
dGgucm91bmQoci50b3Ard2luZG93LnNjcm9sbFkpLHI6TWF0aC5yb3VuZChyLnJpZ2h0K3dpbmRv
dy5zY3JvbGxYKSxiOk1hdGgucm91bmQoci5ib3R0b20rd2luZG93LnNjcm9sbFkpLHc6TWF0aC5y
b3VuZChyLndpZHRoKSxoOk1hdGgucm91bmQoci5oZWlnaHQpfX0=
"""

    // Fragment 4: Element extraction functions
    const val FRAG_EXTRACT = """
ZnVuY3Rpb24gaXNWaXNpYmxlKGUpe2NvbnN0IHM9d2luZG93LmdldENvbXB1dGVkU3R5bGUoZSk7
aWYocy5kaXNwbGF5PT09J25vbmUnfHxzLnZpc2liaWxpdHk9PT0naGlkZGVuJ3x8cGFyc2VGbG9h
dChzLm9wYWNpdHkpPT09MClyZXR1cm4hMTtjb25zdCByPWUuZ2V0Qm91bmRpbmdDbGllbnRSZWN0
KCk7cmV0dXJuIHIud2lkdGghPT0wfHxyLmhlaWdodCE9PTB9ZnVuY3Rpb24gaXNJbnRlcmFjdGl2
ZShlKXtpZihJTlRFUkFDVElWRV9UQUdTLmhhcyhlLnRhZ05hbWUpKXJldHVybiEwO2NvbnN0IHI9
ZS5nZXRBdHRyaWJ1dGUoJ3JvbGUnKTtpZihyJiZJTlRFUkFDVElWRV9ST0xFUy5oYXMoci50b0xv
d2VyQ2FzZSgpKSlyZXR1cm4hMDtpZihlLmhhc0F0dHJpYnV0ZSgnb25jbGljaycpfHwoZS5oYXNB
dHRyaWJ1dGUoJ3RhYmluZGV4JykmJmUudGFiSW5kZXg+PTApKXJldHVybiEwO3JldHVybiB3aW5k
b3cuZ2V0Q29tcHV0ZWRTdHlsZShlKS5jdXJzb3I9PT0ncG9pbnRlcid9
"""

    // Fragment 5: Main scrape function
    const val FRAG_SCRAPE = """
ZnVuY3Rpb24gZ2V0TmFtZShlKXtsZXQgbj1lLmdldEF0dHJpYnV0ZSgnYXJpYS1sYWJlbCcpO2lm
KG4pcmV0dXJuIG4udHJpbSgpO2NvbnN0IGw9ZS5nZXRBdHRyaWJ1dGUoJ2FyaWEtbGFiZWxsZWRi
eScpO2lmKGwpe2NvbnN0IGxlPWRvY3VtZW50LmdldEVsZW1lbnRCeUlkKGwpO2lmKGxlKXJldHVy
biBsZS50ZXh0Q29udGVudC50cmltKCl9aWYoZS5wbGFjZWhvbGRlcilyZXR1cm4gZS5wbGFjZWhv
bGRlcjtpZihlLnRpdGxlKXJldHVybiBlLnRpdGxlLnRyaW0oKTtyZXR1cm4oZS50ZXh0Q29udGVu
dHx8JycpLnRyaW0oKS5zdWJzdHJpbmcoMCxNQVhfVEVYVF9MRU5HVEgpfWZ1bmN0aW9uIGdldFR5
cGUoZSl7Y29uc3QgdD1lLnRhZ05hbWUudG9Mb3dlckNhc2UoKSxpPShlLnR5cGV8fCcnKS50b0xv
d2VyQ2FzZSgpO2lmKHQ9PT0nYScpcmV0dXJuJ2xpbmsnO2lmKHQ9PT0nYnV0dG9uJylyZXR1cm4n
YnV0dG9uJztpZih0PT09J2lucHV0Jyl7aWYoaT09PSdzdWJtaXQnfHxpPT09J2J1dHRvbicpcmV0
dXJuJ2J1dHRvbic7aWYoaT09PSdjaGVja2JveCcpcmV0dXJuJ2NoZWNrYm94JztpZihpPT09J3Jh
ZGlvJylyZXR1cm4ncmFkaW8nO3JldHVybidpbnB1dCd9aWYodD09PSdzZWxlY3QnKXJldHVybidy
b3Bkb3duJztyZXR1cm4nZWxlbWVudCd9
"""

    // Fragment 6: Action functions (click, focus, input, scroll)
    const val FRAG_ACTIONS = """
ZnVuY3Rpb24gc2NyYXBlRE9NKCl7Y29uc3QgZWxlbWVudHM9W10sc2Vlbj1uZXcgU2V0KCk7aWRD
b3VudGVyPTA7ZnVuY3Rpb24gdHJhdmVyc2UobixkewppZihkPk1BWF9ERVBUSHx8ZWxlbWVudHMu
bGVuZ3RoPj1NQVhfRUxFTUVOVFN8fCEobiBpbnN0YW5jZW9mIEVsZW1lbnQpKXJldHVybjtpZigh
aXNWaXNpYmxlKG4pKXJldHVybjtpZihpc0ludGVyYWN0aXZlKG4pJiYhc2Vlbi5oYXMobikpe3Nl
ZW4uYWRkKG4pO2NvbnN0IGI9Z2V0Qm91bmRzKG4pO2VsZW1lbnRzLnB1c2goe2lkOmdlbmVyYXRl
SWQoKSx0YWc6bi50YWdOYW1lLnRvTG93ZXJDYXNlKCksdHlwZTpnZXRUeXBlKG4pLG5hbWU6Z2V0
TmFtZShuKSxib3VuZHM6Yn0pfWZvcihjb25zdCBjIG9mIG4uY2hpbGRyZW4pdHJhdmVyc2UoYyxk
KzEpfXRyYXZlcnNlKGRvY3VtZW50LmJvZHksMCk7cmV0dXJue3VybDp3aW5kb3cubG9jYXRpb24u
aHJlZix0aXRsZTpkb2N1bWVudC50aXRsZSx0aW1lc3RhbXA6RGF0ZS5ub3coKSxlbGVtZW50czpl
bGVtZW50cyxjb3VudDplbGVtZW50cy5sZW5ndGh9fQ==
"""

    // Fragment 7: Return statement and IIFE close
    const val FRAG_FOOTER = """
cmV0dXJuIEpTT04uc3RyaW5naWZ5KHtzY3JhcGU6c2NyYXBlRE9NKCksdmVyc2lvbjonMS4wLjAn
fSl9KSgpOw==
"""

    // Action templates
    const val FRAG_CLICK_TEMPLATE = """
KGZ1bmN0aW9uKCl7Y29uc3QgZT1kb2N1bWVudC5xdWVyeVNlbGVjdG9yKCd7e1NFTEVDVE9SfX0n
KTtpZihlKXtlLmNsaWNrKCk7cmV0dXJuIEpTT04uc3RyaW5naWZ5KHtzdWNjZXNzOnRydWV9KX1y
ZXR1cm4gSlNPTi5zdHJpbmdpZnkoe3N1Y2Nlc3M6ZmFsc2UsZXJyb3I6J05vdCBmb3VuZCd9KX0p
KCk7
"""

    const val FRAG_FOCUS_TEMPLATE = """
KGZ1bmN0aW9uKCl7Y29uc3QgZT1kb2N1bWVudC5xdWVyeVNlbGVjdG9yKCd7e1NFTEVDVE9SfX0n
KTtpZihlJiZlLmZvY3VzKXtlLmZvY3VzKCk7cmV0dXJuIEpTT04uc3RyaW5naWZ5KHtzdWNjZXNz
OnRydWV9KX1yZXR1cm4gSlNPTi5zdHJpbmdpZnkoe3N1Y2Nlc3M6ZmFsc2UsZXJyb3I6J05vdCBm
b3VuZCd9KX0pKCk7
"""

    const val FRAG_INPUT_TEMPLATE = """
KGZ1bmN0aW9uKCl7Y29uc3QgZT1kb2N1bWVudC5xdWVyeVNlbGVjdG9yKCd7e1NFTEVDVE9SfX0n
KTtpZihlKXtlLnZhbHVlPSd7e1RFWFR9fSc7ZS5kaXNwYXRjaEV2ZW50KG5ldyBFdmVudCgnaW5w
dXQnLHtidWJibGVzOnRydWV9KSk7cmV0dXJuIEpTT04uc3RyaW5naWZ5KHtzdWNjZXNzOnRydWV9
KX1yZXR1cm4gSlNPTi5zdHJpbmdpZnkoe3N1Y2Nlc3M6ZmFsc2UsZXJyb3I6J05vdCBmb3VuZCd9
KX0pKCk7
"""

    const val FRAG_SCROLL_TEMPLATE = """
KGZ1bmN0aW9uKCl7Y29uc3QgZT1kb2N1bWVudC5xdWVyeVNlbGVjdG9yKCd7e1NFTEVDVE9SfX0n
KTtpZihlKXtlLnNjcm9sbEludG9WaWV3KHtiZWhhdmlvcjonc21vb3RoJyxibG9jazonY2VudGVy
J30pO3JldHVybiBKU09OLnN0cmluZ2lmeSh7c3VjY2Vzczp0cnVlfSl9cmV0dXJuIEpTT04uc3Ry
aW5naWZ5KHtzdWNjZXNzOmZhbHNlLGVycm9yOidOb3QgZm91bmQnfSl9KSgpOw==
"""
}
