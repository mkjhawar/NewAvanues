/**
 * VosSftpClient.kt - SFTP client wrapper using JSch for VOS file sync
 *
 * Provides connect/disconnect, upload/download, list, and manifest operations.
 * All I/O runs in Dispatchers.IO. 30s connect timeout. StrictHostKeyChecking=no for dev.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-11
 */
package com.augmentalis.voiceoscore.vos.sync

import android.util.Log
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpATTRS
import com.jcraft.jsch.SftpProgressMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Properties
import java.util.Vector

private const val TAG = "VosSftpClient"
private const val CONNECT_TIMEOUT_MS = 30_000
private const val CHANNEL_TIMEOUT_MS = 15_000
private const val MANIFEST_FILENAME = "manifest.json"

class VosSftpClient {

    private var session: Session? = null
    private var channel: ChannelSftp? = null
    private val jsch = JSch()

    fun isConnected(): Boolean = session?.isConnected == true && channel?.isConnected == true

    /**
     * Connect to SFTP server.
     *
     * @param hostKeyChecking Host key verification mode:
     *   - "no": Skip verification (dev only, MITM risk)
     *   - "accept-new": Trust on first connect, reject changes (recommended for testing)
     *   - "yes": Strict verification against known_hosts (production)
     */
    suspend fun connect(
        host: String,
        port: Int,
        username: String,
        authMode: SftpAuthMode,
        hostKeyChecking: String = "accept-new"
    ): SftpResult<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()

            when (authMode) {
                is SftpAuthMode.SshKey -> {
                    val keyFile = File(authMode.keyFilePath)
                    if (!keyFile.exists()) {
                        return@withContext SftpResult.Error("SSH key file not found")
                    }
                    if (authMode.passphrase.isNotEmpty()) {
                        jsch.addIdentity(authMode.keyFilePath, authMode.passphrase)
                    } else {
                        jsch.addIdentity(authMode.keyFilePath)
                    }
                }
                is SftpAuthMode.Password -> {
                    // Password will be set on session
                }
            }

            val newSession = jsch.getSession(username, host, port).apply {
                if (authMode is SftpAuthMode.Password) {
                    setPassword(authMode.password)
                }
                val config = Properties().apply {
                    put("StrictHostKeyChecking", hostKeyChecking)
                    put("PreferredAuthentications", when (authMode) {
                        is SftpAuthMode.SshKey -> "publickey"
                        is SftpAuthMode.Password -> "password"
                    })
                }
                setConfig(config)
                timeout = CONNECT_TIMEOUT_MS
            }

            newSession.connect(CONNECT_TIMEOUT_MS)
            session = newSession

            val sftpChannel = newSession.openChannel("sftp") as ChannelSftp
            sftpChannel.connect(CHANNEL_TIMEOUT_MS)
            channel = sftpChannel

            Log.i(TAG, "Connected to $host:$port")
            SftpResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed: ${e.message}", e)
            disconnect()
            SftpResult.Error("Connection failed: ${e.message}", e)
        }
    }

    /**
     * Disconnect from SFTP server.
     */
    fun disconnect() {
        try {
            channel?.disconnect()
            session?.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Error during disconnect: ${e.message}")
        } finally {
            channel = null
            session = null
        }
    }

    /**
     * Upload a local file to the remote path.
     * Creates parent directories on the server if needed.
     */
    suspend fun uploadFile(
        localPath: String,
        remotePath: String,
        onProgress: ((Long, Long) -> Unit)? = null
    ): SftpResult<Unit> = withContext(Dispatchers.IO) {
        val ch = channel ?: return@withContext SftpResult.Error("Not connected")
        try {
            val localFile = File(localPath)
            if (!localFile.exists()) {
                return@withContext SftpResult.Error("Local file not found: $localPath")
            }

            // Ensure remote directory exists
            val remoteDir = remotePath.substringBeforeLast('/')
            ensureRemoteDir(ch, remoteDir)

            val totalSize = localFile.length()
            val monitor = if (onProgress != null) {
                object : SftpProgressMonitor {
                    private var transferred = 0L
                    override fun init(op: Int, src: String?, dest: String?, max: Long) {}
                    override fun count(count: Long): Boolean {
                        transferred += count
                        onProgress(transferred, totalSize)
                        return true
                    }
                    override fun end() {}
                }
            } else null

            ch.put(localPath, remotePath, monitor, ChannelSftp.OVERWRITE)
            Log.d(TAG, "Uploaded ${File(localPath).name} (${totalSize} bytes)")
            SftpResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed: ${e.message}", e)
            SftpResult.Error("Upload failed: ${e.message}", e)
        }
    }

    /**
     * Download a remote file to a local path.
     */
    suspend fun downloadFile(
        remotePath: String,
        localPath: String,
        onProgress: ((Long, Long) -> Unit)? = null
    ): SftpResult<Unit> = withContext(Dispatchers.IO) {
        val ch = channel ?: return@withContext SftpResult.Error("Not connected")
        try {
            // Get remote file size for progress tracking
            val attrs = ch.stat(remotePath)
            val totalSize = attrs.size

            // Ensure local directory exists
            val localDir = File(localPath).parentFile
            if (localDir != null && !localDir.exists()) {
                localDir.mkdirs()
            }

            val monitor = if (onProgress != null) {
                object : SftpProgressMonitor {
                    private var transferred = 0L
                    override fun init(op: Int, src: String?, dest: String?, max: Long) {}
                    override fun count(count: Long): Boolean {
                        transferred += count
                        onProgress(transferred, totalSize)
                        return true
                    }
                    override fun end() {}
                }
            } else null

            ch.get(remotePath, localPath, monitor)
            Log.d(TAG, "Downloaded ${File(localPath).name} ($totalSize bytes)")
            SftpResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            SftpResult.Error("Download failed: ${e.message}", e)
        }
    }

    /**
     * List files in a remote directory.
     */
    suspend fun listFiles(remotePath: String): SftpResult<List<RemoteFileInfo>> = withContext(Dispatchers.IO) {
        val ch = channel ?: return@withContext SftpResult.Error("Not connected")
        try {
            @Suppress("UNCHECKED_CAST")
            val entries = ch.ls(remotePath) as Vector<ChannelSftp.LsEntry>
            val files = entries
                .filter { !it.attrs.isDir && it.filename != "." && it.filename != ".." }
                .map { entry ->
                    RemoteFileInfo(
                        name = entry.filename,
                        size = entry.attrs.size,
                        modifiedTime = entry.attrs.mTime.toLong() * 1000L
                    )
                }
            SftpResult.Success(files)
        } catch (e: Exception) {
            Log.e(TAG, "List failed: ${e.message}", e)
            SftpResult.Error("List failed: ${e.message}", e)
        }
    }

    /**
     * Fetch and parse the server manifest (manifest.json) from the remote root.
     */
    suspend fun fetchManifest(remotePath: String): SftpResult<ServerManifest> = withContext(Dispatchers.IO) {
        val ch = channel ?: return@withContext SftpResult.Error("Not connected")
        try {
            val manifestPath = "$remotePath/$MANIFEST_FILENAME"

            // Check if manifest exists
            try {
                ch.stat(manifestPath)
            } catch (_: Exception) {
                // No manifest yet — return empty
                return@withContext SftpResult.Success(ServerManifest())
            }

            val outputStream = ByteArrayOutputStream()
            ch.get(manifestPath, outputStream)
            val json = outputStream.toString(Charsets.UTF_8.name())

            val manifest = parseManifest(json)
            Log.d(TAG, "Fetched manifest: ${manifest.files.size} entries")
            SftpResult.Success(manifest)
        } catch (e: Exception) {
            Log.e(TAG, "Fetch manifest failed: ${e.message}", e)
            SftpResult.Error("Fetch manifest failed: ${e.message}", e)
        }
    }

    /**
     * Upload a manifest to the remote root.
     */
    suspend fun uploadManifest(
        manifest: ServerManifest,
        remotePath: String
    ): SftpResult<Unit> = withContext(Dispatchers.IO) {
        val ch = channel ?: return@withContext SftpResult.Error("Not connected")
        try {
            val json = serializeManifest(manifest)
            val manifestPath = "$remotePath/$MANIFEST_FILENAME"

            ensureRemoteDir(ch, remotePath)

            val inputStream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))
            ch.put(inputStream, manifestPath, ChannelSftp.OVERWRITE)
            inputStream.close()

            Log.d(TAG, "Uploaded manifest with ${manifest.files.size} entries")
            SftpResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Upload manifest failed: ${e.message}", e)
            SftpResult.Error("Upload manifest failed: ${e.message}", e)
        }
    }

    /**
     * Recursively ensure a remote directory exists.
     */
    private fun ensureRemoteDir(ch: ChannelSftp, path: String) {
        val parts = path.split("/").filter { it.isNotEmpty() }
        var current = ""
        for (part in parts) {
            current = "$current/$part"
            try {
                ch.stat(current)
            } catch (_: Exception) {
                try {
                    ch.mkdir(current)
                } catch (_: Exception) {
                    // May already exist due to race condition
                }
            }
        }
    }

    private fun parseManifest(json: String): ServerManifest {
        val root = JSONObject(json)
        val version = root.optString("version", "1.0")
        val lastUpdated = root.optLong("lastUpdated", 0L)
        val filesArray = root.optJSONArray("files") ?: JSONArray()

        val files = (0 until filesArray.length()).map { i ->
            val entry = filesArray.getJSONObject(i)
            ManifestEntry(
                hash = entry.getString("hash"),
                filename = entry.getString("filename"),
                size = entry.getLong("size"),
                uploadedAt = entry.getLong("uploadedAt"),
                domain = entry.optString("domain").takeIf { it.isNotEmpty() },
                locale = entry.optString("locale").takeIf { it.isNotEmpty() }
            )
        }

        return ServerManifest(version = version, files = files, lastUpdated = lastUpdated)
    }

    private fun serializeManifest(manifest: ServerManifest): String {
        val root = JSONObject()
        root.put("version", manifest.version)
        root.put("lastUpdated", manifest.lastUpdated)

        val filesArray = JSONArray()
        for (entry in manifest.files) {
            val obj = JSONObject()
            obj.put("hash", entry.hash)
            obj.put("filename", entry.filename)
            obj.put("size", entry.size)
            obj.put("uploadedAt", entry.uploadedAt)
            if (entry.domain != null) obj.put("domain", entry.domain)
            if (entry.locale != null) obj.put("locale", entry.locale)
            filesArray.put(obj)
        }
        root.put("files", filesArray)

        return root.toString(2)
    }
}
