pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AVA-AI"

// Android App (in app/ subdirectory)
include(":app")
project(":app").projectDir = file("app")

// AVA Modules (from Modules/AVA/)
// Core modules
include(":core:Utils")
project(":core:Utils").projectDir = file("../../../Modules/AVA/core/Utils")

include(":core:Domain")
project(":core:Domain").projectDir = file("../../../Modules/AVA/core/Domain")

include(":core:Data")
project(":core:Data").projectDir = file("../../../Modules/AVA/core/Data")

include(":core:Theme")
project(":core:Theme").projectDir = file("../../../Modules/AVA/core/Theme")

// Shared modules (shared between AVA and VoiceOS)
include(":SharedPlatform")
project(":SharedPlatform").projectDir = file("../../../Modules/Shared/Platform")

include(":SharedNLU")
project(":SharedNLU").projectDir = file("../../../Modules/Shared/NLU")

// Feature modules

include(":Chat")
project(":Chat").projectDir = file("../../../Modules/AVA/Chat")

include(":Teach")
project(":Teach").projectDir = file("../../../Modules/AVA/Teach")

include(":Overlay")
project(":Overlay").projectDir = file("../../../Modules/AVA/Overlay")

include(":LLM")
project(":LLM").projectDir = file("../../../Modules/LLM")

include(":RAG")
project(":RAG").projectDir = file("../../../Modules/RAG")

include(":Actions")
project(":Actions").projectDir = file("../../../Modules/AVA/Actions")

include(":WakeWord")
project(":WakeWord").projectDir = file("../../../Modules/AVA/WakeWord")

include(":Memory")
project(":Memory").projectDir = file("../../../Modules/AVA/memory")
