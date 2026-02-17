/**
 * PlatformUtils.ios.kt - iOS-specific platform utilities
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-18
 * Updated: 2026-02-12 - Fixed Kotlin 2.1.0 Native compatibility (atomicfu synchronized)
 */
package com.augmentalis.speechrecognition

import platform.Foundation.NSDate
import platform.Foundation.NSLog
import platform.Foundation.timeIntervalSince1970
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Get current time in milliseconds (iOS)
 */
actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()

/**
 * Log a debug message (iOS)
 */
actual fun logDebug(tag: String, message: String) {
    NSLog("D/$tag: $message")
}

/**
 * Log an info message (iOS)
 */
actual fun logInfo(tag: String, message: String) {
    NSLog("I/$tag: $message")
}

/**
 * Log a warning message (iOS)
 */
actual fun logWarn(tag: String, message: String) {
    NSLog("W/$tag: $message")
}

/**
 * Log an error message (iOS)
 */
actual fun logError(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        NSLog("E/$tag: $message - ${throwable.message}")
    } else {
        NSLog("E/$tag: $message")
    }
}

/**
 * Create a thread-safe mutable list (iOS/Native)
 * Uses atomicfu SynchronizedObject for Kotlin/Native thread safety
 */
actual fun <T> createSynchronizedList(): MutableList<T> {
    return SynchronizedMutableList()
}

/**
 * Create a thread-safe mutable map (iOS/Native)
 */
actual fun <K, V> createConcurrentMap(): MutableMap<K, V> {
    return SynchronizedMutableMap()
}

/**
 * Simple synchronized list implementation for iOS
 * Uses atomicfu SynchronizedObject instead of JVM synchronized(Any())
 */
private class SynchronizedMutableList<T> : MutableList<T> {
    private val list = mutableListOf<T>()
    private val lock = SynchronizedObject()

    override val size: Int get() = synchronized(lock) { list.size }

    override fun contains(element: T): Boolean = synchronized(lock) { list.contains(element) }

    override fun containsAll(elements: Collection<T>): Boolean = synchronized(lock) { list.containsAll(elements) }

    override fun get(index: Int): T = synchronized(lock) { list[index] }

    override fun indexOf(element: T): Int = synchronized(lock) { list.indexOf(element) }

    override fun isEmpty(): Boolean = synchronized(lock) { list.isEmpty() }

    override fun iterator(): MutableIterator<T> = synchronized(lock) { list.toMutableList().iterator() }

    override fun lastIndexOf(element: T): Int = synchronized(lock) { list.lastIndexOf(element) }

    override fun add(element: T): Boolean = synchronized(lock) { list.add(element) }

    override fun add(index: Int, element: T) = synchronized(lock) { list.add(index, element) }

    override fun addAll(index: Int, elements: Collection<T>): Boolean = synchronized(lock) { list.addAll(index, elements) }

    override fun addAll(elements: Collection<T>): Boolean = synchronized(lock) { list.addAll(elements) }

    override fun clear() = synchronized(lock) { list.clear() }

    override fun listIterator(): MutableListIterator<T> = synchronized(lock) { list.toMutableList().listIterator() }

    override fun listIterator(index: Int): MutableListIterator<T> = synchronized(lock) { list.toMutableList().listIterator(index) }

    override fun remove(element: T): Boolean = synchronized(lock) { list.remove(element) }

    override fun removeAll(elements: Collection<T>): Boolean = synchronized(lock) { list.removeAll(elements) }

    override fun removeAt(index: Int): T = synchronized(lock) { list.removeAt(index) }

    override fun retainAll(elements: Collection<T>): Boolean = synchronized(lock) { list.retainAll(elements) }

    override fun set(index: Int, element: T): T = synchronized(lock) { list.set(index, element) }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> = synchronized(lock) { list.subList(fromIndex, toIndex).toMutableList() }
}

/**
 * Simple synchronized map implementation for iOS
 * Uses atomicfu SynchronizedObject instead of JVM synchronized(Any())
 */
private class SynchronizedMutableMap<K, V> : MutableMap<K, V> {
    private val map = mutableMapOf<K, V>()
    private val lock = SynchronizedObject()

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = synchronized(lock) { map.entries.toMutableSet() }

    override val keys: MutableSet<K>
        get() = synchronized(lock) { map.keys.toMutableSet() }

    override val size: Int
        get() = synchronized(lock) { map.size }

    override val values: MutableCollection<V>
        get() = synchronized(lock) { map.values.toMutableList() }

    override fun clear() = synchronized(lock) { map.clear() }

    override fun isEmpty(): Boolean = synchronized(lock) { map.isEmpty() }

    override fun remove(key: K): V? = synchronized(lock) { map.remove(key) }

    override fun putAll(from: Map<out K, V>) = synchronized(lock) { map.putAll(from) }

    override fun put(key: K, value: V): V? = synchronized(lock) { map.put(key, value) }

    override fun get(key: K): V? = synchronized(lock) { map[key] }

    override fun containsValue(value: V): Boolean = synchronized(lock) { map.containsValue(value) }

    override fun containsKey(key: K): Boolean = synchronized(lock) { map.containsKey(key) }
}
