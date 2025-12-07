plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.augmentalis.universalipc"
    compileSdk = 34  // Android 14 (API 34) - Latest stable

    defaultConfig {
        minSdk = 29  // Android 10 (Q) - Minimum supported
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests {
            all {
                it.jvmArgs("-Dnet.bytebuddy.experimental=true")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Compatibility Support
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.annotation:annotation:1.7.1")

    // JSON Serialization (for DSL parsing)
    implementation("com.google.code.gson:gson:2.10.1")

    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.25")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.mockito:mockito-inline:4.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Android Test dependencies
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.augmentalis"
            artifactId = "universalipc"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
