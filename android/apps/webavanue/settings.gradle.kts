pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    }
}

rootProject.name = "WebAvanue"

// Android App
include(":app")

// WebAvanue Modules (from Modules/WebAvanue/)
include(":universal")
project(":universal").projectDir = file("../../../Modules/WebAvanue/universal")

include(":coredata")
project(":coredata").projectDir = file("../../../Modules/WebAvanue/coredata")
