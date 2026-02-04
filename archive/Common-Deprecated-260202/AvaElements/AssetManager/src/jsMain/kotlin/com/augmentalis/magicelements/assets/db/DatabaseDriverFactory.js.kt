package com.augmentalis.avaelements.assets.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

/**
 * Web (JavaScript) database driver factory
 *
 * Uses WebWorkerDriver for SQLite in the browser via sql.js
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        // Create worker for sql.js
        val worker = Worker(
            js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)""") as String
        )

        return WebWorkerDriver(
            worker = worker
        ).also { driver ->
            // Create schema
            AssetDatabase.Schema.create(driver)
        }
    }
}
