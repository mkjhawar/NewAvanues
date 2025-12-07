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
    val uuid = UUID.randomUUID().toString()
    val directory = file("${layout.buildDirectory}/generated/assets/model-en-us")
    val file = file("$directory/uuid")
    doLast {
        mkdir(directory)
        file.writeText(uuid)
    }
}

tasks.named("preBuild") {
    dependsOn("genUUID")
}
