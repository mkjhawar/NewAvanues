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

// Feature modules
// NLU is now in Shared (shared between AVA and VoiceOS)
include(":SharedNLU")
project(":SharedNLU").projectDir = file("../../../Modules/Shared/NLU")

include(":Chat")
project(":Chat").projectDir = file("../../../Modules/AVA/Chat")

include(":Teach")
project(":Teach").projectDir = file("../../../Modules/AVA/Teach")

include(":Overlay")
project(":Overlay").projectDir = file("../../../Modules/AVA/Overlay")

include(":LLM")
project(":LLM").projectDir = file("../../../Modules/AVA/LLM")

include(":RAG")
project(":RAG").projectDir = file("../../../Modules/AVA/RAG")

include(":Actions")
project(":Actions").projectDir = file("../../../Modules/AVA/Actions")

include(":WakeWord")
project(":WakeWord").projectDir = file("../../../Modules/AVA/WakeWord")
