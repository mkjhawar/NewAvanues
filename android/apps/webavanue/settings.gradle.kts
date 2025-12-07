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

// WebAvanue Modules (in Modules/WebAvanue/)
include(":Modules:WebAvanue:universal")
project(":Modules:WebAvanue:universal").projectDir = file("../../../Modules/WebAvanue/universal")

include(":Modules:WebAvanue:coredata")
project(":Modules:WebAvanue:coredata").projectDir = file("../../../Modules/WebAvanue/coredata")
