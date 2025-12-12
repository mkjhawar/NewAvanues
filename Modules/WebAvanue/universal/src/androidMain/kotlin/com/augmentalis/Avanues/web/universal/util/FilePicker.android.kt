package com.augmentalis.Avanues.web.universal.util

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * Android implementation of FilePicker.
 *
 * Uses Android Storage Access Framework (SAF) for file picking and saving.
 * Requires an Activity context for launching file picker intents.
 *
 * @property context Android activity context
 */
class AndroidFilePicker(private val context: ComponentActivity) : FilePicker {

    private var pickFileContinuation: ((FilePickerResult?) -> Unit)? = null
    private var saveFileContinuation: ((SaveFileResult) -> Unit)? = null

    private val pickFileLauncher: ActivityResultLauncher<Array<String>> =
        context.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            handlePickedFile(uri)
        }

    private val createFileLauncher: ActivityResultLauncher<String> =
        context.registerForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
            handleCreatedFile(uri)
        }

    override suspend fun pickFile(
        mimeTypes: List<String>,
        callback: (FilePickerResult?) -> Unit
    ) {
        pickFileContinuation = callback
        pickFileLauncher.launch(mimeTypes.toTypedArray())
    }

    override suspend fun saveFile(
        filename: String,
        content: String,
        mimeType: String,
        callback: (SaveFileResult) -> Unit
    ) {
        saveFileContinuation = callback
        tempFileContent = content
        createFileLauncher.launch(filename)
    }

    override suspend fun shareFile(
        filename: String,
        content: String,
        mimeType: String
    ) {
        try {
            // Create temporary file in cache
            val cacheDir = context.cacheDir
            val file = File(cacheDir, filename)
            file.writeText(content)

            // Get URI using FileProvider
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            // Create share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Bookmarks")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Bookmarks"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handlePickedFile(uri: Uri?) {
        val continuation = pickFileContinuation
        pickFileContinuation = null

        if (uri == null) {
            continuation?.invoke(null)
            return
        }

        try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().decodeToString()
            } ?: run {
                continuation?.invoke(null)
                return
            }

            val filename = getFileName(uri) ?: "bookmarks.html"

            continuation?.invoke(
                FilePickerResult(
                    filename = filename,
                    content = content,
                    uri = uri.toString()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            continuation?.invoke(null)
        }
    }

    private fun handleCreatedFile(uri: Uri?) {
        val continuation = saveFileContinuation
        val content = tempFileContent
        saveFileContinuation = null
        tempFileContent = null

        if (uri == null || content == null) {
            continuation?.invoke(SaveFileResult(success = false, filePath = null, error = "Cancelled"))
            return
        }

        try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.toByteArray())
            }

            continuation?.invoke(
                SaveFileResult(
                    success = true,
                    filePath = uri.toString(),
                    error = null
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            continuation?.invoke(
                SaveFileResult(
                    success = false,
                    filePath = null,
                    error = e.message
                )
            )
        }
    }

    private fun getFileName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) it.getString(nameIndex) else null
            } else null
        }
    }

    companion object {
        private var tempFileContent: String? = null
    }
}

/**
 * Android actual implementation of createFilePicker.
 * Requires storing activity context globally or passing it as parameter.
 */
private var filePickerContext: ComponentActivity? = null

/**
 * Initialize file picker with activity context.
 * Must be called before using createFilePicker().
 *
 * @param activity ComponentActivity instance
 */
fun initializeFilePicker(activity: ComponentActivity) {
    filePickerContext = activity
}

actual fun createFilePicker(): FilePicker {
    val context = filePickerContext
        ?: throw IllegalStateException("FilePicker not initialized. Call initializeFilePicker(activity) first.")
    return AndroidFilePicker(context)
}

actual fun getDownloadsDirectory(): String {
    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
}
