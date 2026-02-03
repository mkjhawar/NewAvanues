import java.util.UUID

plugins {
    id("com.android.library")
}

android {
    namespace = "com.voiceos.vosk_models"
    compileSdk = 34

    sourceSets {
        named("main") {
            assets.srcDirs(files("${layout.buildDirectory}/generated/assets"))
        }
    }
}

tasks.register("genUUID") {
    val directory = layout.buildDirectory.dir("generated/assets/model-en-us")
    val uuidFile = directory.map { it.file("uuid") }

    outputs.file(uuidFile)

    doLast {
        val dir = directory.get().asFile
        dir.mkdirs()
        val uuid = UUID.randomUUID().toString()
        uuidFile.get().asFile.writeText(uuid)
    }
}

tasks.named("preBuild") {
    dependsOn("genUUID")
}
