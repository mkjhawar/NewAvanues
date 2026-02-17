package com.augmentalis.webavanue

/**
 * iOS DownloadFilePickerLauncher stub implementation
 *
 * iOS file picker integration requires UIDocumentPickerViewController
 */
actual class DownloadFilePickerLauncher {
    actual fun launch(
        suggestedFilename: String,
        mimeType: String,
        onResult: (String?) -> Unit
    ) {
        // iOS document picker requires UIViewController integration
        // For now, save to documents directory directly
        val documentsPath = platform.Foundation.NSSearchPathForDirectoriesInDomains(
            platform.Foundation.NSDocumentDirectory,
            platform.Foundation.NSUserDomainMask,
            true
        ).firstOrNull() as? String

        val filePath = documentsPath?.let { "$it/$suggestedFilename" }
        onResult(filePath)
    }

    actual fun isAvailable(): Boolean {
        return true
    }
}
