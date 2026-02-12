package com.augmentalis.webavanue

import platform.Foundation.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.*
import kotlinx.cinterop.*

/**
 * iOS FilePicker implementation using UIDocumentPickerViewController
 */
class IOSFilePicker : FilePicker {

    override suspend fun pickFile(
        mimeTypes: List<String>,
        callback: (FilePickerResult?) -> Unit
    ) {
        // Convert MIME types to UTTypes
        val utTypes = mimeTypes.map { mimeType ->
            when (mimeType) {
                "text/html" -> UTTypeHTML.identifier
                "text/plain" -> UTTypePlainText.identifier
                "image/*" -> UTTypeImage.identifier
                "application/pdf" -> UTTypePDF.identifier
                "*/*" -> UTTypeData.identifier
                else -> UTTypeData.identifier
            }
        }

        // iOS file picker requires UIViewController integration
        // For now, return null (would need view controller reference)
        callback(null)
    }

    override suspend fun saveFile(
        filename: String,
        content: String,
        mimeType: String,
        callback: (SaveFileResult) -> Unit
    ) {
        // Save to documents directory
        val documentsPath = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).firstOrNull() as? String

        if (documentsPath != null) {
            val filePath = "$documentsPath/$filename"
            val nsContent = content as NSString
            val success = nsContent.writeToFile(
                filePath,
                atomically = true,
                encoding = NSUTF8StringEncoding,
                error = null
            )

            if (success) {
                callback(SaveFileResult(
                    success = true,
                    filePath = filePath
                ))
            } else {
                callback(SaveFileResult(
                    success = false,
                    filePath = null,
                    error = "Failed to write file"
                ))
            }
        } else {
            callback(SaveFileResult(
                success = false,
                filePath = null,
                error = "Documents directory not found"
            ))
        }
    }

    override suspend fun shareFile(
        filename: String,
        content: String,
        mimeType: String
    ) {
        // iOS share sheet requires UIViewController integration
        // Would need to present UIActivityViewController
        println("Share file not implemented for iOS (requires UIViewController)")
    }
}

/**
 * Create iOS file picker
 */
actual fun createFilePicker(): FilePicker {
    return IOSFilePicker()
}

/**
 * Get downloads directory for iOS
 */
actual fun getDownloadsDirectory(): String {
    val documentsPath = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true
    ).firstOrNull() as? String

    return documentsPath ?: NSTemporaryDirectory()
}
