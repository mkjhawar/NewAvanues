package com.augmentalis.voiceos.speech.engines.vivoka

/**
 * File: app/src/main/java/com/voiceos/app/audio/VsdkHandlerUtils.kt
 *
 * Copyright © 2024 Intelligent Devices LLC/Augmentalis/Manoj Jhawar, Aman Jhawar
 * Protected under one or more issued or pending patents
 *
 * Created: 2024-09-09 10:00:00 PST
 */


import java.io.File


/**
 * Utility class for handling Vivoka SDK asset files.
 * This class provides methods to copy necessary assets from the app's assets directory
 * to the application's file system, ensuring that the Vivoka SDK has access to required files.
 */
class VsdkHandlerUtils(private val assetsPath: String) {

    /**
     * Checks if the necessary Vivoka SDK files exist in the application's file system.
     *
     * @return `true` if all required directories are present and not empty, `false` otherwise.
     */
    fun checkVivokaFilesExist(): Boolean {
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
    }
}

