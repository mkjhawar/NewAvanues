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

rootProject.name = "MainAvanues"

// Android Apps
include(":android:apps:webavanue")
project(":android:apps:webavanue").projectDir = file("android/apps/webavanue")

// Common Libraries - WebAvanue
include(":common:webavanue:universal")
project(":common:webavanue:universal").projectDir = file("common/webavanue/universal")

include(":common:webavanue:coredata")
project(":common:webavanue:coredata").projectDir = file("common/webavanue/coredata")

// Legacy modules - commented out, to be deleted
// include(":common:libs:webview:android")
// include(":common:libs:webview:ios")
// include(":common:libs:webview:desktop")
// includeBuild("Modules/WebAvanue")
