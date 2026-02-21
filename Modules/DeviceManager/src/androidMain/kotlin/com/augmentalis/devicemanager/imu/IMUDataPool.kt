package com.augmentalis.devicemanager.imu

import java.util.LinkedList

class IMUDataPool(capacity: Int) {
    private val pool = LinkedList<IMUData>()

    init {
        // Pre-populate pool
        repeat(capacity) {
            pool.add(IMUData(0f, 0f, 0f, 0))
        }
    }

    fun acquire(): IMUData {
        return synchronized(pool) {
            if (pool.isEmpty()) {
                IMUData(0f, 0f, 0f, 0)
            } else {
                pool.removeFirst()
            }
        }
    }

    fun release(data: IMUData) {
        synchronized(pool) {
            // Reset values
            data.alpha = 0f
            data.beta = 0f
            data.gamma = 0f
            data.ts = 0

            // Return to pool
            if (pool.size < 20) { // Cap pool size
                pool.add(data)
            }
        }
    }
}

data class IMUData(var alpha: Float, var beta: Float, var gamma: Float, var ts: Long)

/** Acquire an [IMUData] from the pool, run [block], and release it back automatically. */
inline fun <T> IMUDataPool.use(block: (IMUData) -> T): T {
    val data = acquire()
    return try {
        block(data)
    } finally {
        release(data)
    }
}