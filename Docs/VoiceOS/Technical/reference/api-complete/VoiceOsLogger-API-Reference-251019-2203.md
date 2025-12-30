# VoiceOsLogger - Complete API Reference

**Module Type:** libraries
**Generated:** 2025-10-19 22:04:01 PDT
**Timestamp:** 251019-2203
**Location:** `modules/libraries/VoiceOsLogger`

---

## Overview

This document provides a complete file-by-file, class-by-class reference for the VoiceOsLogger module.

## Files


### File: `src/main/java/com/augmentalis/logger/remote/FirebaseLogger.kt`

**Package:** `com.augmentalis.logger.remote`

**Classes/Interfaces/Objects:**
  - class FirebaseLogger 

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import com.augmentalis.logger.VoiceOsLogger

---

### File: `src/main/java/com/augmentalis/logger/remote/HttpLogTransport.kt`

**Package:** `com.augmentalis.logger.remote`

**Classes/Interfaces/Objects:**
  - class HttpLogTransport(

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.withContext
  - import java.io.OutputStreamWriter
  - import java.net.HttpURLConnection
  - import java.net.URL

---

### File: `src/main/java/com/augmentalis/logger/remote/LogTransport.kt`

**Package:** `com.augmentalis.logger.remote`

**Classes/Interfaces/Objects:**
  - interface LogTransport 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/logger/remote/RemoteLogSender.kt`

**Package:** `com.augmentalis.logger.remote`

**Classes/Interfaces/Objects:**
  - class RemoteLogSender(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.os.Build
  - import android.util.Log
  - import com.augmentalis.logger.VoiceOsLogger
  - import kotlinx.coroutines.*
  - import org.json.JSONArray
  - import org.json.JSONObject
  - import java.util.concurrent.ConcurrentLinkedQueue

---

### File: `src/main/java/com/augmentalis/logger/VoiceOsLogger.kt`

**Package:** `com.augmentalis.logger`

**Classes/Interfaces/Objects:**
  - object VoiceOsLogger 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.logger.remote.FirebaseLogger
  - import com.augmentalis.logger.remote.RemoteLogSender
  - import kotlinx.coroutines.*
  - import java.io.File
  - import java.text.SimpleDateFormat
  - import java.util.*
  - import java.util.concurrent.ConcurrentLinkedQueue

---

### File: `src/test/java/com/augmentalis/logger/remote/LogEntryTest.kt`

**Package:** `com.augmentalis.logger.remote`

**Classes/Interfaces/Objects:**
  - class LogEntryTest 

**Public Functions:**

**Imports:**
  - import org.junit.Test
  - import org.junit.Assert.*

---

### File: `src/test/java/com/augmentalis/logger/remote/LogTransportTest.kt`

**Package:** `com.augmentalis.logger.remote`

**Classes/Interfaces/Objects:**
  - class LogTransportTest 
  - class MockLogTransport(
  - class MockGrpcTransport(

**Public Functions:**

**Imports:**
  - import kotlinx.coroutines.runBlocking
  - import org.junit.Assert.*
  - import org.junit.Before
  - import org.junit.Test

---

### File: `src/test/java/com/augmentalis/logger/remote/RemoteLogSenderTest.kt`

**Package:** `com.augmentalis.logger.remote`

**Classes/Interfaces/Objects:**
  - class RemoteLogSenderTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.logger.VoiceOsLogger
  - import kotlinx.coroutines.delay
  - import kotlinx.coroutines.runBlocking
  - import org.junit.Assert.*
  - import org.junit.Before
  - import org.junit.Test
  - import org.junit.runner.RunWith
  - import org.robolectric.RobolectricTestRunner
  - import org.robolectric.RuntimeEnvironment

---

## Summary

**Total Files:** 8

**Module Structure:**
```
                  build
                    generated
                      res
                        pngs
                          debug
                        resValues
                          debug
                    intermediates
                      aapt_friendly_merged_manifests
                        debug
                          processDebugManifest
                            aapt
                      aar_metadata
                        debug
                          writeDebugAarMetadata
                      annotation_processor_list
                        debug
                          javaPreCompileDebug
                      compile_library_classes_jar
                        debug
                          bundleLibCompileToJarDebug
                      compile_r_class_jar
                        debug
                          generateDebugRFile
                      compile_symbol_list
                        debug
                          generateDebugRFile
                      compiled_local_resources
                        debug
                          compileDebugLibraryResources
                            out
                      data_binding_layout_info_type_package
                        debug
                          packageDebugResources
                            out
                      incremental
                        debug
                          packageDebugResources
                            merged.dir
                            stripped.dir
                        mergeDebugJniLibFolders
                        mergeDebugShaders
                        packageDebugAssets
                      java_res
                        debug
                          processDebugJavaRes
                            out
                              com
                                augmentalis
                                  logger
```

---

**Generated by:** VOS4 API Documentation Generator
**Timestamp:** 251019-2203
