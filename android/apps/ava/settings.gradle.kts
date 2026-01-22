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
include(":Utilities")
project(":Utilities").projectDir = file("../../../Modules/Utilities")

include(":NLU")
project(":NLU").projectDir = file("../../../Modules/AI/NLU")

// Feature modules

include(":Chat")
project(":Chat").projectDir = file("../../../Modules/AI/Chat")

include(":Teach")
project(":Teach").projectDir = file("../../../Modules/AI/Teach")

include(":Overlay")
project(":Overlay").projectDir = file("../../../Modules/AVA/Overlay")

include(":LLM")
project(":LLM").projectDir = file("../../../Modules/AI/LLM")

include(":RAG")
project(":RAG").projectDir = file("../../../Modules/AI/RAG")

include(":Actions")
project(":Actions").projectDir = file("../../../Modules/Actions")

include(":WakeWord")
project(":WakeWord").projectDir = file("../../../Modules/Voice/WakeWord")

include(":Memory")
project(":Memory").projectDir = file("../../../Modules/AI/Memory")
