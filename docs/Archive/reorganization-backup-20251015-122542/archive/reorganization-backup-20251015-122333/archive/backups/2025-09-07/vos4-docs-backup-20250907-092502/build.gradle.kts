// Top-level build file - Updated to latest stable versions
plugins {
    // Updated to compatible versions with Gradle 8.11.1
    id("com.android.application") version "8.7.0" apply false
    id("com.android.library") version "8.7.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false  // Updated from 1.9.24
    id("org.jetbrains.kotlin.plugin.parcelize") version "1.9.25" apply false
    // Note: Compose plugin only available in Kotlin 2.0+, using kotlinCompilerExtensionVersion instead
    id("com.google.devtools.ksp") version "1.9.25-1.0.20" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.25" apply false  // KAPT for Room
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}

//allprojects {
//    repositories {
//        google()
//        mavenCentral()
//        maven { url = uri("https://alphacephei.com/maven/") } // Vosk
//    }
//}

// Apply testing rules to all modules
// Temporarily disabled while fixing test task creation issue
// apply(from = "gradle/simple-testing.gradle.kts")

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

// Test tasks are now enabled with fixed dependencies

// Force dependency resolution to avoid version conflicts
allprojects {
    configurations.all {
        resolutionStrategy {
            // Force consistent Compose versions
            force("androidx.compose.ui:ui:1.6.8")
            force("androidx.compose.runtime:runtime:1.6.8")
            force("androidx.compose.ui:ui-graphics:1.6.8")
            force("androidx.compose.ui:ui-tooling-preview:1.6.8")
            force("androidx.compose.material3:material3:1.2.1")
            force("androidx.compose.material:material-icons-extended:1.6.8")
            
            // Force annotation version
            force("androidx.annotation:annotation:1.7.1")
            
            // Align all Compose BOMs to same version
            eachDependency {
                if (requested.group == "androidx.compose" && requested.name == "compose-bom") {
                    useVersion("2024.06.00")
                }
            }
        }
    }
}
