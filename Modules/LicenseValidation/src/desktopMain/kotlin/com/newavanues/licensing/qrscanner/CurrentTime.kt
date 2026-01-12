package com.newavanues.licensing.qrscanner

/**
 * Desktop/JVM implementation of currentTimeMillis using System.currentTimeMillis().
 */
internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()
