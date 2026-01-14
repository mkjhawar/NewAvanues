/**
 * AvidGeneratorDesktop.kt - Desktop (JVM) Platform Implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avid

/**
 * Desktop (JVM) implementation of currentTimeMillis
 */
internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()
