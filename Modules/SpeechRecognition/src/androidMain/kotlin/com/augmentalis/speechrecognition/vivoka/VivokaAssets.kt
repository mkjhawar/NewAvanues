/**
 * VivokaAssets.kt - Asset management and validation for Vivoka VSDK engine
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-28
 * Updated: 2026-01-27 - Migrated to KMP androidMain
 *
 * Extracted from monolithic VivokaEngine.kt as part of SOLID refactoring
 * Handles asset extraction, validation, corruption detection, and recovery
 */
package com.augmentalis.speechrecognition.vivoka

import android.content.Context
import android.util.Log
import com.augmentalis.speechrecognition.vivoka.model.Model
import com.augmentalis.speechrecognition.vivoka.model.Root
import com.google.gson.Gson
import com.vivoka.vsdk.util.AssetsExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader

/**
 * Manages VSDK assets including extraction, validation, and integrity checking
 */
@Deprecated(
    message = "Use SpeechModelPathResolver in VivokaInitializer instead",
    replaceWith = ReplaceWith(""),
    level = DeprecationLevel.WARNING
)
class VivokaAssets(
    private val context: Context
) {

    // Asset paths
    private var assetsPath: String = ""

    data class AssetValidationResult(
        val isValid: Boolean,
        val reason: String,
        val needsReExtraction: Boolean = false,
        val validatedFiles: Int = 0,
        val totalFiles: Int = 0
    )

    /**
     * Initialize asset management with assets path
     */
    fun initialize(assetsPath: String) {
        this.assetsPath = assetsPath
        Log.d(TAG, "Asset management initialized for path: $assetsPath")
    }

    /**
     * Extract and validate VSDK assets with comprehensive checks
     *
     * @deprecated Legacy asset extraction from APK - replaced by SpeechModelPathResolver
     *
     * As of v2.1.0, model path resolution is handled by VivokaInitializer using
     * SpeechModelPathResolver which checks multiple locations (internal, external, shared)
     * before attempting extraction. This method is kept for backward compatibility but
     * should not be called in normal flow.
     *
     * See: VivokaInitializer.initialize() for current implementation
     */
    @Deprecated(
        message = "Use SpeechModelPathResolver in VivokaInitializer instead",
        replaceWith = ReplaceWith(""),
        level = DeprecationLevel.WARNING
    )
    suspend fun extractAndValidateAssets(): AssetValidationResult {
        return withContext(Dispatchers.IO) {
            try {
                // DEPRECATED: Asset extraction now handled by VivokaInitializer
                // This code path should not be reached in v2.1.0+
                // Keeping for backward compatibility only

                /* LEGACY CODE - COMMENTED OUT
                if (!checkVivokaFilesExist()) {
                    AssetsExtractor.extract(context, "vsdk", assetsPath)
                    return@withContext AssetValidationResult(true, "")
                }
                */

                // Return success if files already exist (from path resolver)
                if (checkVivokaFilesExist()) {
                    return@withContext AssetValidationResult(
                        isValid = true,
                        reason = "VSDK files found via path resolver",
                        needsReExtraction = false,
                        validatedFiles = 1,
                        totalFiles = 1
                    )
                }

                // Models not found - path resolver should have handled this
                return@withContext AssetValidationResult(
                    isValid = false,
                    reason = "VSDK files not found - check VivokaInitializer path resolution",
                    needsReExtraction = false
                )

            } catch (e: Exception) {
                Log.e(TAG, "Asset validation check failed", e)
                return@withContext AssetValidationResult(
                    isValid = false,
                    reason = "Validation error: ${e.message}",
                    needsReExtraction = false
                )
            }
        }
    }

    /**
     * Checks if the necessary Vivoka SDK files exist in the application's file system.
     *
     * @return `true` if all required directories are present and not empty, `false` otherwise.
     */
    private  fun checkVivokaFilesExist(): Boolean {
        return listOf(
            CONFIG, DATA, DATA_CSDK, DATA_CSDK_ASR, DATA_CSDK_ASR_ACMOD,
            DATA_CSDK_ASR_ASR, DATA_CSDK_ASR_CLC, DATA_CSDK_ASR_CTX, DATA_CSDK_ASR_LM
        ).all {
            isFolderNotEmpty("$assetsPath$it")
        }
    }

    /**
     * Checks if the specified folder exists and is not empty.
     *
     * @param folderPath The path to the folder.
     * @return `true` if the folder exists and contains files or directories, `false` otherwise.
     */
    private fun isFolderNotEmpty(folderPath: String): Boolean {
        val folder = File(folderPath)
        return folder.exists() && folder.isDirectory && folder.list()?.isNotEmpty() == true
    }


    fun mergeJsonFiles(downloadedFile: String): String? {
        try {// Step 1: Read the existing JSON from internal storage
            val vsdkFile = getConfigFilePath()
            val vsdkContent = FileReader(vsdkFile).readText()
            val gson = Gson()
            // Load and parse local english file from assets
            val vsdkContentRoot = gson.fromJson(vsdkContent, Root::class.java)

            Log.i(TAG, "mergeJsonFiles: vsdkContentRoot = $vsdkContentRoot")

            // Load and parse download files
            val downloadRoot = gson.fromJson(downloadedFile, Root::class.java)

            Log.i(TAG, "mergeJsonFiles: frenchRoot = $downloadRoot")
            // Merge acmods
            vsdkContentRoot.csdk.asr.recognizers.rec.acmods.addAll(downloadRoot.csdk.asr.recognizers.rec.acmods)

            // Merge models from French and Spanish into English
            downloadRoot.csdk.asr.models.forEach { (name, value) ->
                value.let {
                    vsdkContentRoot.csdk.asr.models[name] = mergeModels(
                        vsdkContentRoot.csdk.asr.models[name],
                        it
                    )
                }
            }

            // Save the final merged JSON as final.json in internal storage
            val finalJsonString = gson.toJson(vsdkContentRoot)
            Log.i(TAG, "mergeJsonFiles: finalJsonString = $finalJsonString")
            // Step 4: Write the modified JSON back to the file
            FileOutputStream(vsdkFile).use {
                it.write(finalJsonString.toByteArray())  // The 4 spaces indentation makes it nicely formatted
            }
            return vsdkFile?.path
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // Helper function to merge two Model objects
    private fun mergeModels(existing: Model?, newModel: Model): Model {
        val mergedExtraModels = (existing?.extraModels ?: mutableMapOf()).apply {
            putAll(newModel.extraModels ?: emptyMap())
        }
        val mergedSettings = (existing?.settings ?: mutableMapOf()).apply {
            putAll(newModel.settings ?: emptyMap())
        }
        return Model(
            type = newModel.type,
            file = newModel.file,
            acmod = newModel.acmod,
            extraModels = mergedExtraModels,
            settings = mergedSettings,
            slots = newModel.slots ?: existing?.slots,
            lexicon = newModel.lexicon ?: existing?.lexicon
        )
    }

    fun getConfigFilePath(): File? {
        val parentDir = File(assetsPath)
        if (!parentDir.exists()) {
            parentDir.mkdir()
        }
        val file = File(parentDir, "$CONFIG/$VSDK_CONFIG")
        return if (file.exists() && file.isFile) {
            file
        } else {
            null
        }

    }

    fun reset() {
        Log.d(TAG, "Resetting asset management")
        assetsPath = ""
    }

    companion object {

        const val VSDK_CONFIG = "vsdk.json"

        // Constants representing directories to include or exclude during asset copying
        private const val VOCALIZER = "vocalizer"
        private const val CONFIG = "config"
        private const val DATA = "data"
        private const val DATA_CSDK = "$DATA/csdk"
        private const val DATA_CSDK_ASR = "$DATA_CSDK/asr"
        private const val DATA_CSDK_ASR_ACMOD = "$DATA_CSDK_ASR/acmod"
        private const val DATA_CSDK_ASR_ASR = "$DATA_CSDK_ASR/asr"
        private const val DATA_CSDK_ASR_CLC = "$DATA_CSDK_ASR/clc"
        private const val DATA_CSDK_ASR_CTX = "$DATA_CSDK_ASR/ctx"
        private const val DATA_CSDK_ASR_LM = "$DATA_CSDK_ASR/lm"

        private const val TAG = "VivokaAssets"
    }
}
