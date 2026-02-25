package com.augmentalis.nlu

/**
 * Cross-platform @Synchronized annotation
 *
 * On JVM: Maps to kotlin.jvm.Synchronized (real lock-based synchronization)
 * On JS: No-op (single-threaded event loop, no concurrent access)
 * On Native: No-op (use atomicfu or explicit Mutex for thread safety)
 *
 * This allows commonMain code to use @Synchronized without breaking
 * compilation on non-JVM targets.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
expect annotation class Synchronized()
