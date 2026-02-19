package com.augmentalis.httpavanue.platform

/**
 * TLS configuration for secure socket connections
 */
data class TlsConfig(
    val enabled: Boolean = false,
    val requireClientCert: Boolean = false,
    val protocols: List<String> = listOf("TLSv1.3", "TLSv1.2"),
    val cipherSuites: List<String>? = null,
    val certificateChain: String? = null,
    val privateKey: String? = null,
    val trustedCertificates: String? = null,
    val verifyHostname: Boolean = true,
    val allowSelfSigned: Boolean = false,
) {
    fun validate(isServer: Boolean) {
        if (!enabled) return
        if (isServer) {
            require(certificateChain != null) { "Server certificate chain required for TLS server" }
            require(privateKey != null) { "Server private key required for TLS server" }
        }
        if (requireClientCert) {
            require(trustedCertificates != null) { "Trusted CA certificates required for client certificate validation" }
        }
    }

    companion object {
        fun development(certificateChain: String, privateKey: String) = TlsConfig(
            enabled = true, requireClientCert = false, allowSelfSigned = true,
            verifyHostname = false, certificateChain = certificateChain, privateKey = privateKey,
        )

        fun production(certificateChain: String, privateKey: String, trustedCertificates: String) = TlsConfig(
            enabled = true, requireClientCert = true, allowSelfSigned = false,
            verifyHostname = true, certificateChain = certificateChain,
            privateKey = privateKey, trustedCertificates = trustedCertificates,
        )

        fun disabled() = TlsConfig(enabled = false)

        fun enabled() = TlsConfig(
            enabled = true, requireClientCert = false,
            verifyHostname = true, allowSelfSigned = false,
        )
    }
}
