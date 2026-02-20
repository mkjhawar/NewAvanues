package com.augmentalis.httpavanue.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.BufferedSink
import okio.BufferedSource
import okio.buffer
import okio.sink
import okio.source
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.net.ServerSocket
import java.security.KeyStore
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*
import java.net.Socket as JvmSocket

private object TlsHelper {
    init { if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) Security.addProvider(BouncyCastleProvider()) }

    fun createSslContext(config: TlsConfig, isServer: Boolean): SSLContext {
        val sslContext = SSLContext.getInstance("TLS")
        val keyManagers = if (isServer && config.certificateChain != null && config.privateKey != null)
            createKeyManagers(config.certificateChain!!, config.privateKey!!) else null
        val trustManagers = when {
            config.trustedCertificates != null -> createTrustManagers(config.trustedCertificates!!)
            config.allowSelfSigned -> arrayOf(createTrustAllManager())
            else -> null
        }
        sslContext.init(keyManagers, trustManagers, null)
        return sslContext
    }

    private fun createKeyManagers(certChainPem: String, privateKeyPem: String): Array<KeyManager> {
        val converter = JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
        val keyConverter = JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
        val certificates = mutableListOf<X509Certificate>()
        PEMParser(StringReader(certChainPem)).use { parser ->
            var obj = parser.readObject(); while (obj != null) { if (obj is X509CertificateHolder) certificates.add(converter.getCertificate(obj)); obj = parser.readObject() }
        }
        require(certificates.isNotEmpty()) { "No certificates found in PEM" }
        val privateKey = PEMParser(StringReader(privateKeyPem)).use { parser ->
            when (val obj = parser.readObject()) { is PrivateKeyInfo -> keyConverter.getPrivateKey(obj); else -> throw IllegalArgumentException("Unsupported private key format") }
        }
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()); keyStore.load(null, null)
        val keyPassword = ByteArray(32).apply { java.security.SecureRandom().nextBytes(this) }.joinToString("") { "%02x".format(it) }.toCharArray()
        keyStore.setKeyEntry("server", privateKey, keyPassword, certificates.toTypedArray())
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()); kmf.init(keyStore, keyPassword)
        keyPassword.fill('0')
        return kmf.keyManagers
    }

    private fun createTrustManagers(trustedCertsPem: String): Array<TrustManager> {
        val certFactory = CertificateFactory.getInstance("X.509")
        val certs = certFactory.generateCertificates(ByteArrayInputStream(trustedCertsPem.toByteArray()))
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()); keyStore.load(null, null)
        certs.forEachIndexed { index, cert -> keyStore.setCertificateEntry("ca-$index", cert) }
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); tmf.init(keyStore)
        return tmf.trustManagers
    }

    private fun createTrustAllManager() = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    fun configureSocket(sslSocket: SSLSocket, config: TlsConfig, isClient: Boolean) {
        sslSocket.enabledProtocols = config.protocols.toTypedArray()
        config.cipherSuites?.let { sslSocket.enabledCipherSuites = it.toTypedArray() }
        if (!isClient && config.requireClientCert) sslSocket.needClientAuth = true
    }
}

actual class Socket(private val jvmSocket: JvmSocket, config: SocketConfig = SocketConfig()) {
    private val bufferedSource: BufferedSource
    private val bufferedSink: BufferedSink
    init {
        jvmSocket.apply { soTimeout = config.readTimeout.toInt(); keepAlive = config.keepAlive; tcpNoDelay = config.tcpNoDelay; receiveBufferSize = config.receiveBufferSize; sendBufferSize = config.sendBufferSize }
        bufferedSource = jvmSocket.getInputStream().source().buffer()
        bufferedSink = jvmSocket.getOutputStream().sink().buffer()
    }
    actual companion object {
        actual suspend fun connect(host: String, port: Int, config: SocketConfig): Socket = withContext(Dispatchers.IO) {
            config.tls.validate(isServer = false)
            val socket = if (config.tls.enabled) {
                val sslContext = TlsHelper.createSslContext(config.tls, isServer = false)
                val sslSocket = sslContext.socketFactory.createSocket(host, port) as SSLSocket
                TlsHelper.configureSocket(sslSocket, config.tls, isClient = true)
                if (config.tls.verifyHostname) { val session = sslSocket.session; if (!HttpsURLConnection.getDefaultHostnameVerifier().verify(host, session)) { sslSocket.close(); throw SSLPeerUnverifiedException("Hostname verification failed for $host") } }
                sslSocket
            } else JvmSocket().apply { connect(java.net.InetSocketAddress(host, port), config.readTimeout.toInt()) }
            Socket(socket, config)
        }
    }
    actual fun source() = bufferedSource
    actual fun sink() = bufferedSink
    actual fun close() { bufferedSource.close(); bufferedSink.close(); jvmSocket.close() }
    actual fun isConnected() = jvmSocket.isConnected && !jvmSocket.isClosed
    actual fun remoteAddress() = "${jvmSocket.inetAddress.hostAddress}:${jvmSocket.port}"
    actual fun setReadTimeout(timeoutMs: Long) { jvmSocket.soTimeout = timeoutMs.toInt() }
}

actual class SocketServer actual constructor(private val config: SocketConfig) {
    private var serverSocket: ServerSocket? = null
    actual fun bind(port: Int, backlog: Int) {
        config.tls.validate(isServer = true)
        serverSocket = if (config.tls.enabled) {
            val sslContext = TlsHelper.createSslContext(config.tls, isServer = true)
            (sslContext.serverSocketFactory.createServerSocket(port, backlog) as SSLServerSocket).apply {
                enabledProtocols = config.tls.protocols.toTypedArray(); config.tls.cipherSuites?.let { enabledCipherSuites = it.toTypedArray() }
                if (config.tls.requireClientCert) needClientAuth = true; reuseAddress = true; soTimeout = 0
            }
        } else ServerSocket(port, backlog).apply { reuseAddress = true; soTimeout = 0 }
    }
    actual suspend fun accept(): Socket = withContext(Dispatchers.IO) { Socket(serverSocket?.accept() ?: throw IllegalStateException("Server not bound"), config) }
    actual fun close() { serverSocket?.close(); serverSocket = null }
    actual fun isBound() = serverSocket?.isBound == true
    actual fun localPort() = serverSocket?.localPort ?: -1
}
