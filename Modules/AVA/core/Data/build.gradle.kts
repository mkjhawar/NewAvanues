plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
    // KSP removed - was only used for Room compiler
}

android {
    namespace = "com.augmentalis.ava.core.data"
    compileSdk = 34

    defaultConfig {
        minSdk = 28  // Android 9+ (Pie and above)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Core modules
    implementation(project(":core:Domain"))
    implementation(project(":core:Utils"))

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // SQLDelight - KMP database (primary)
    // Room has been removed - SQLDelight is now the only database layer
    api(libs.sqldelight.runtime)
    api(libs.sqldelight.android.driver)
    api(libs.sqldelight.coroutines.extensions)

    // DataStore (for developer preferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Dagger Hilt for dependency injection
    implementation("com.google.dagger:hilt-android:2.48")
    implementation("androidx.hilt:hilt-common:1.1.0")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    // Room testing removed - using SQLDelight
    testImplementation("org.robolectric:robolectric:4.11")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test:core-ktx:1.5.0")

    // Android Instrumented Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:core-ktx:1.5.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation(libs.sqldelight.android.driver)
    androidTestImplementation(libs.sqldelight.coroutines.extensions)
}

// SQLDelight Configuration
sqldelight {
    databases {
        create("AVADatabase") {
            packageName.set("com.augmentalis.ava.core.data.db")
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.0.1")
            schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
            // Use CREATE TABLE statements in .sq files directly
            // Set to true only when migration files (.sqm) exist
            deriveSchemaFromMigrations.set(false)
        }
    }
}
