plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

group = "com.augmentalis.magicui"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // LSP4J - Language Server Protocol implementation
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.21.1")

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Kotlin serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Reuse existing AVAMagic components
    implementation(project(":Modules:AVAMagic:MagicUI:Components:ThemeBuilder"))
    implementation(project(":Modules:AVAMagic:MagicUI:CodeGen:Parser"))
    implementation(project(":Modules:AVAMagic:MagicUI:CodeGen:Generators"))
    implementation(project(":Modules:VoiceOS:libraries:VUIDCreator"))
    implementation(project(":Modules:AVAMagic:MagicUI:Components:Core"))

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
}

application {
    mainClass.set("com.augmentalis.magicui.lsp.MagicUILanguageServerLauncher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.augmentalis.magicui.lsp.MagicUILanguageServerLauncher"
    }

    // Create fat JAR with all dependencies
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

kotlin {
    jvmToolchain(17)
}
